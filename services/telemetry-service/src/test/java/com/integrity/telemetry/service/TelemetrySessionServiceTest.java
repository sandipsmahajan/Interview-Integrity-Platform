package com.integrity.telemetry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.integrity.exception.NotFoundException;
import com.integrity.exception.ValidationFailedException;
import com.integrity.telemetry.domain.TelemetrySession;
import com.integrity.telemetry.domain.TelemetrySessionStatus;
import com.integrity.telemetry.repository.TelemetrySessionRepository;
import com.integrity.telemetry.repository.TelemetrySummaryRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the telemetry session service. */
class TelemetrySessionServiceTest {

  private final TelemetrySessionRepository sessionRepository =
      Mockito.mock(TelemetrySessionRepository.class);
  private final TelemetrySummaryRepository summaryRepository =
      Mockito.mock(TelemetrySummaryRepository.class);

  private TelemetrySessionService sessionService;

  @BeforeEach
  void setUp() {
    sessionService = new TelemetrySessionService(sessionRepository, summaryRepository);
  }

  private static TelemetrySession persisted(UUID id, UUID organizationId) {
    return new TelemetrySession(
        id,
        organizationId,
        UUID.randomUUID(),
        null,
        "browser",
        "1.0",
        TelemetrySessionStatus.ACTIVE,
        5,
        java.time.Instant.now(),
        null,
        java.time.Instant.now(),
        java.time.Instant.now(),
        1);
  }

  @Test
  void createReturnsInsertedSession() {
    UUID organizationId = UUID.randomUUID();
    UUID interviewId = UUID.randomUUID();
    when(sessionRepository.insert(any(TelemetrySession.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(sessionService.create(organizationId, interviewId, null, null, null, 30))
        .assertNext(
            session -> {
              assertThat(session.getOrganizationId()).isEqualTo(organizationId);
              assertThat(session.getInterviewId()).isEqualTo(interviewId);
              assertThat(session.getStatus()).isEqualTo(TelemetrySessionStatus.STARTED);
              assertThat(session.getHeartbeatCadenceSeconds()).isEqualTo(30);
            })
        .verifyComplete();
  }

  @Test
  void getReturnsNotFoundForUnknownSession() {
    UUID id = UUID.randomUUID();
    when(sessionRepository.findById(id)).thenReturn(Mono.empty());

    StepVerifier.create(sessionService.get(UUID.randomUUID(), id))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void getRejectsCrossTenantSession() {
    UUID id = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    when(sessionRepository.findById(id)).thenReturn(Mono.just(persisted(id, organizationId)));

    StepVerifier.create(sessionService.get(UUID.randomUUID(), id))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void changeStatusMovesSessionForward() {
    UUID id = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    TelemetrySession session = persisted(id, organizationId);
    when(sessionRepository.findById(id)).thenReturn(Mono.just(session));
    when(sessionRepository.updateStatus(eq(id), eq(organizationId), any(), any()))
        .thenAnswer(
            invocation ->
                Mono.just(
                    new TelemetrySession(
                        id,
                        organizationId,
                        session.getInterviewId(),
                        null,
                        "browser",
                        "1.0",
                        invocation.getArgument(2),
                        session.getHeartbeatCadenceSeconds(),
                        session.getStartedAt(),
                        null,
                        session.getCreatedAt(),
                        java.time.Instant.now(),
                        2)));

    StepVerifier.create(
            sessionService.changeStatus(organizationId, id, TelemetrySessionStatus.ENDED, null))
        .assertNext(
            updated -> assertThat(updated.getStatus()).isEqualTo(TelemetrySessionStatus.ENDED))
        .verifyComplete();
  }

  @Test
  void changeStatusRejectsIllegalTransition() {
    UUID id = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    when(sessionRepository.findById(id)).thenReturn(Mono.just(persisted(id, organizationId)));

    StepVerifier.create(
            sessionService.changeStatus(organizationId, id, TelemetrySessionStatus.STARTED, null))
        .expectError(ValidationFailedException.class)
        .verify();
  }

  @SuppressWarnings("unchecked")
  @Test
  void ensureSessionInsertsWhenMissing() {
    UUID organizationId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    UUID interviewId = UUID.randomUUID();
    when(sessionRepository.findById(sessionId))
        .thenReturn(Mono.empty(), Mono.just(persisted(sessionId, organizationId)));
    when(sessionRepository.insertWithId(any(TelemetrySession.class))).thenReturn(Mono.empty());

    StepVerifier.create(
            sessionService.ensureSession(
                organizationId, sessionId, interviewId, null, "device", "1.0", 5))
        .assertNext(session -> assertThat(session.getId()).isEqualTo(sessionId))
        .verifyComplete();
  }

  @Test
  void ensureSessionRefreshesExistingClientInfo() {
    UUID organizationId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    TelemetrySession existing = persisted(sessionId, organizationId);
    when(sessionRepository.findById(sessionId)).thenReturn(Mono.just(existing));
    when(sessionRepository.updateClientInfo(eq(sessionId), eq(organizationId), any(), any(), any()))
        .thenReturn(Mono.just(existing));

    StepVerifier.create(
            sessionService.ensureSession(
                organizationId, sessionId, UUID.randomUUID(), null, "device", "1.1", 5))
        .assertNext(session -> assertThat(session.getId()).isEqualTo(sessionId))
        .verifyComplete();
    verify(sessionRepository)
        .updateClientInfo(eq(sessionId), eq(organizationId), any(), any(), any());
  }
}
