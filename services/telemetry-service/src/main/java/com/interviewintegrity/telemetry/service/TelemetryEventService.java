package com.interviewintegrity.telemetry.service;

import com.interviewintegrity.telemetry.domain.TelemetryEvent;
import com.interviewintegrity.telemetry.domain.TelemetrySession;
import com.interviewintegrity.telemetry.repository.TelemetryEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Ingests and queries raw telemetry events for a session. */
public class TelemetryEventService {

  private static final Logger log = LoggerFactory.getLogger(TelemetryEventService.class);
  private static final String PROCTOR_ALERT = "PROCTOR_ALERT";

  private final TelemetryEventRepository eventRepository;
  private final TelemetrySessionService sessionService;
  private final TelemetryViolationPublisher violationPublisher;

  /** Wires the service with its collaborators. */
  public TelemetryEventService(
      TelemetryEventRepository eventRepository,
      TelemetrySessionService sessionService,
      TelemetryViolationPublisher violationPublisher) {
    this.eventRepository = eventRepository;
    this.sessionService = sessionService;
    this.violationPublisher = violationPublisher;
  }

  /**
   * Stores a batch of events for a session, returning the stored rows.
   *
   * <p>Ingestion is idempotent per {@code (eventId, occurredAt)}. Proctor alerts within the batch
   * are additionally forwarded to the policy engine; a forwarding failure never fails ingestion.
   */
  @Transactional
  public Flux<TelemetryEvent> ingest(
      UUID organizationId, UUID sessionId, List<TelemetryEventData> events) {
    if (events == null || events.isEmpty()) {
      return Flux.empty();
    }
    return sessionService
        .get(organizationId, sessionId)
        .flatMapMany(session -> ingestEvents(organizationId, session, events));
  }

  /** Lists the events of a session, optionally filtered by event type. */
  public Flux<TelemetryEvent> list(UUID organizationId, UUID sessionId, String eventType) {
    if (eventType == null) {
      return eventRepository.listBySession(organizationId, sessionId);
    }
    return eventRepository.listBySessionAndType(organizationId, sessionId, eventType);
  }

  /** Counts the events of a session. */
  public Mono<Long> count(UUID organizationId, UUID sessionId) {
    return eventRepository.countBySession(organizationId, sessionId);
  }

  private Flux<TelemetryEvent> ingestEvents(
      UUID organizationId, TelemetrySession session, List<TelemetryEventData> events) {
    Flux<TelemetryEvent> stored =
        Flux.fromIterable(events)
            .flatMap(
                event -> {
                  UUID eventId = event.eventId() != null ? event.eventId() : UUID.randomUUID();
                  String payload = event.payload() != null ? event.payload() : "{}";
                  return eventRepository
                      .insertEvent(
                          eventId,
                          organizationId,
                          session.getId(),
                          session.getInterviewId(),
                          event.eventType(),
                          event.seq(),
                          event.occurredAt(),
                          event.clientOccurredAt(),
                          payload)
                      .thenReturn(
                          new TelemetryEvent(
                              eventId,
                              organizationId,
                              session.getId(),
                              session.getInterviewId(),
                              event.eventType(),
                              event.seq(),
                              event.occurredAt(),
                              event.clientOccurredAt(),
                              payload));
                },
                8);
    return stored.concatWith(
        forwardProctorAlerts(organizationId, session, events).thenMany(Flux.empty()));
  }

  private Mono<Void> forwardProctorAlerts(
      UUID organizationId, TelemetrySession session, List<TelemetryEventData> events) {
    return Flux.fromIterable(events)
        .filter(event -> PROCTOR_ALERT.equals(event.eventType()))
        .flatMap(
            event ->
                publishAlert(organizationId, session, event)
                    .onErrorResume(
                        error -> {
                          if (log.isWarnEnabled()) {
                            log.warn(
                                "Failed to forward proctor alert for session {}: {}",
                                session.getId(),
                                error.getMessage());
                          }
                          return Mono.empty();
                        }))
        .then();
  }

  private Mono<Void> publishAlert(
      UUID organizationId, TelemetrySession session, TelemetryEventData event) {
    String evidence = event.payload() != null ? event.payload() : "{}";
    Instant occurred = event.occurredAt() != null ? event.occurredAt() : Instant.now();
    TelemetryViolationEvent violation =
        new TelemetryViolationEvent(
            session.getId(),
            session.getInterviewId(),
            null,
            event.eventType(),
            "MEDIUM",
            "Proctor alert received for session " + session.getId(),
            evidence,
            occurred,
            "telemetry-service");
    return violationPublisher.publishViolation(organizationId, violation);
  }
}
