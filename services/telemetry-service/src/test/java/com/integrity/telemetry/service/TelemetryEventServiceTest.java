package com.integrity.telemetry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.integrity.telemetry.domain.TelemetrySession;
import com.integrity.telemetry.domain.TelemetrySessionStatus;
import com.integrity.telemetry.repository.TelemetryEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the telemetry event service. */
class TelemetryEventServiceTest {

  private final TelemetryEventRepository eventRepository =
      Mockito.mock(TelemetryEventRepository.class);
  private final TelemetrySessionService sessionService =
      Mockito.mock(TelemetrySessionService.class);
  private final TelemetryViolationPublisher violationPublisher =
      Mockito.mock(TelemetryViolationPublisher.class);

  private TelemetryEventService eventService;

  @BeforeEach
  void setUp() {
    eventService = new TelemetryEventService(eventRepository, sessionService, violationPublisher);
  }

  @Test
  void ingestSkipsNullEvents() {
    StepVerifier.create(eventService.ingest(UUID.randomUUID(), UUID.randomUUID(), null))
        .verifyComplete();
    StepVerifier.create(eventService.ingest(UUID.randomUUID(), UUID.randomUUID(), List.of()))
        .verifyComplete();
  }

  @Test
  void ingestStoresBatchAgainstSession() {
    UUID organizationId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    UUID interviewId = UUID.randomUUID();
    TelemetrySession session = session(organizationId, sessionId, interviewId);
    when(sessionService.get(organizationId, sessionId)).thenReturn(Mono.just(session));
    when(eventRepository.insertEvent(
            any(), any(), any(), any(), any(), anyLong(), any(), any(), any()))
        .thenReturn(Mono.empty());
    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.now();
    TelemetryEventData data =
        new TelemetryEventData(eventId, "KEYSTROKE", 1, occurredAt, occurredAt, "{\"k\":1}");

    StepVerifier.create(eventService.ingest(organizationId, sessionId, List.of(data)))
        .assertNext(
            event -> {
              assertThat(event.getId()).isEqualTo(eventId);
              assertThat(event.getSessionId()).isEqualTo(sessionId);
              assertThat(event.getEventType()).isEqualTo("KEYSTROKE");
              assertThat(event.getSeq()).isEqualTo(1);
            })
        .verifyComplete();
  }

  @Test
  void ingestYieldsNothingWhenSessionLookupIsEmpty() {
    UUID organizationId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    when(sessionService.get(organizationId, sessionId)).thenReturn(Mono.empty());

    StepVerifier.create(
            eventService.ingest(
                organizationId,
                sessionId,
                List.of(new TelemetryEventData(null, "X", 1, null, null, null))))
        .verifyComplete();
  }

  @Test
  void ingestForwardsProctorAlerts() {
    UUID organizationId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    UUID interviewId = UUID.randomUUID();
    TelemetrySession session = session(organizationId, sessionId, interviewId);
    when(sessionService.get(organizationId, sessionId)).thenReturn(Mono.just(session));
    when(eventRepository.insertEvent(
            any(), any(), any(), any(), any(), anyLong(), any(), any(), any()))
        .thenReturn(Mono.empty());
    when(violationPublisher.publishViolation(any(), any())).thenReturn(Mono.empty());
    TelemetryEventData alert =
        new TelemetryEventData(UUID.randomUUID(), "PROCTOR_ALERT", 7, Instant.now(), null, "{}");

    StepVerifier.create(eventService.ingest(organizationId, sessionId, List.of(alert)))
        .expectNextCount(1)
        .verifyComplete();
    verify(violationPublisher).publishViolation(any(), any());
  }

  @Test
  void ingestContinuesWhenAlertForwardingFails() {
    UUID organizationId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    TelemetrySession session = session(organizationId, sessionId, UUID.randomUUID());
    when(sessionService.get(organizationId, sessionId)).thenReturn(Mono.just(session));
    when(eventRepository.insertEvent(
            any(), any(), any(), any(), any(), anyLong(), any(), any(), any()))
        .thenReturn(Mono.empty());
    when(violationPublisher.publishViolation(any(), any()))
        .thenReturn(Mono.error(new IllegalStateException("kafka down")));
    TelemetryEventData alert =
        new TelemetryEventData(UUID.randomUUID(), "PROCTOR_ALERT", 7, Instant.now(), null, "{}");

    StepVerifier.create(eventService.ingest(organizationId, sessionId, List.of(alert)))
        .expectNextCount(1)
        .verifyComplete();
  }

  @Test
  void countDelegatesToRepository() {
    UUID organizationId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    when(eventRepository.countBySession(organizationId, sessionId)).thenReturn(Mono.just(3L));

    StepVerifier.create(eventService.count(organizationId, sessionId))
        .assertNext(count -> assertThat(count).isEqualTo(3))
        .verifyComplete();
  }

  private static TelemetrySession session(UUID organizationId, UUID sessionId, UUID interviewId) {
    return new TelemetrySession(
        sessionId,
        organizationId,
        interviewId,
        null,
        "browser",
        "1.0",
        TelemetrySessionStatus.ACTIVE,
        5,
        Instant.now(),
        null,
        Instant.now(),
        Instant.now(),
        1);
  }
}
