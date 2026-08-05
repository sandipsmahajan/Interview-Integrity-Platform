package com.integrity.telemetry.service;

import com.integrity.exception.NotFoundException;
import com.integrity.telemetry.domain.TelemetryEventSummary;
import com.integrity.telemetry.domain.TelemetrySession;
import com.integrity.telemetry.domain.TelemetrySessionStatus;
import com.integrity.telemetry.repository.TelemetrySessionRepository;
import com.integrity.telemetry.repository.TelemetrySummaryRepository;
import com.integrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages telemetry monitoring sessions. */
public class TelemetrySessionService {

  private final TelemetrySessionRepository sessionRepository;
  private final TelemetrySummaryRepository summaryRepository;

  /** Wires the service with its repositories. */
  public TelemetrySessionService(
      TelemetrySessionRepository sessionRepository, TelemetrySummaryRepository summaryRepository) {
    this.sessionRepository = sessionRepository;
    this.summaryRepository = summaryRepository;
  }

  /** Creates a session for an interview. */
  public Mono<TelemetrySession> create(
      UUID organizationId,
      UUID interviewId,
      UUID candidateId,
      String deviceId,
      String clientVersion,
      Integer heartbeatCadenceSeconds) {
    return sessionRepository.insert(
        new TelemetrySession(
            organizationId,
            interviewId,
            candidateId,
            deviceId,
            clientVersion,
            heartbeatCadenceSeconds));
  }

  /** Returns a single session, validating tenant ownership. */
  public Mono<TelemetrySession> get(UUID organizationId, UUID id) {
    return sessionRepository
        .findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Session not found")))
        .flatMap(session -> assertOrganization(session, organizationId));
  }

  /** Lists the sessions of an organization, newest first. */
  public Flux<TelemetrySession> list(UUID organizationId) {
    return sessionRepository.listByOrganization(organizationId);
  }

  /** Applies a lifecycle transition to a session, enforcing a valid state machine. */
  public Mono<TelemetrySession> changeStatus(
      UUID organizationId, UUID id, TelemetrySessionStatus status, Instant endedAt) {
    return get(organizationId, id)
        .flatMap(
            session -> {
              Assert.isTrue(
                  canTransition(session.getStatus(), status),
                  "Cannot transition session from " + session.getStatus() + " to " + status);
              Instant end = endedAt;
              if ((status == TelemetrySessionStatus.ENDED
                      || status == TelemetrySessionStatus.ABANDONED)
                  && end == null) {
                end = Instant.now();
              }
              return sessionRepository.updateStatus(id, organizationId, status, end);
            });
  }

  /**
   * Ensures a session exists for a client-managed id, refreshing client identity when it already
   * exists.
   */
  public Mono<TelemetrySession> ensureSession(
      UUID organizationId,
      UUID sessionId,
      UUID interviewId,
      UUID candidateId,
      String deviceId,
      String clientVersion,
      Integer heartbeatCadenceSeconds) {
    return sessionRepository
        .findById(sessionId)
        .flatMap(
            existing -> {
              if (!organizationId.equals(existing.getOrganizationId())) {
                return Mono.error(new NotFoundException("Session not found"));
              }
              return sessionRepository.updateClientInfo(
                  sessionId, organizationId, deviceId, clientVersion, existing.getStatus());
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  TelemetrySession session =
                      new TelemetrySession(
                          organizationId,
                          interviewId,
                          candidateId,
                          deviceId,
                          clientVersion,
                          heartbeatCadenceSeconds);
                  session.setId(sessionId);
                  return sessionRepository
                      .insertWithId(session)
                      .then(sessionRepository.findById(sessionId));
                }));
  }

  /** Returns the hourly rollups of a session. */
  public Flux<TelemetryEventSummary> summaries(UUID organizationId, UUID sessionId) {
    return summaryRepository.listBySession(organizationId, sessionId);
  }

  private static boolean canTransition(
      TelemetrySessionStatus current, TelemetrySessionStatus target) {
    if (target == current) {
      return true;
    }
    return switch (current) {
      case STARTED ->
          target == TelemetrySessionStatus.ACTIVE
              || target == TelemetrySessionStatus.ENDED
              || target == TelemetrySessionStatus.ABANDONED;
      case ACTIVE ->
          target == TelemetrySessionStatus.ENDED || target == TelemetrySessionStatus.ABANDONED;
      case ENDED, ABANDONED -> false;
    };
  }

  private Mono<TelemetrySession> assertOrganization(TelemetrySession session, UUID organizationId) {
    return Mono.justOrEmpty(session)
        .flatMap(
            s -> {
              if (!organizationId.equals(s.getOrganizationId())) {
                return Mono.error(new NotFoundException("Session not found"));
              }
              return Mono.just(s);
            });
  }
}
