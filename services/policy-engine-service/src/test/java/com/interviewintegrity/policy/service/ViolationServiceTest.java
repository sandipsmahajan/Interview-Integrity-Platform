package com.interviewintegrity.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.exception.ValidationFailedException;
import com.interviewintegrity.policy.domain.ReviewAction;
import com.interviewintegrity.policy.domain.Violation;
import com.interviewintegrity.policy.domain.ViolationSeverity;
import com.interviewintegrity.policy.domain.ViolationStatus;
import com.interviewintegrity.policy.repository.ViolationEscalationRepository;
import com.interviewintegrity.policy.repository.ViolationRepository;
import com.interviewintegrity.policy.repository.ViolationReviewRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the violation service. */
class ViolationServiceTest {

  private final ViolationRepository violationRepository = Mockito.mock(ViolationRepository.class);
  private final ViolationReviewRepository reviewRepository =
      Mockito.mock(ViolationReviewRepository.class);
  private final ViolationEscalationRepository escalationRepository =
      Mockito.mock(ViolationEscalationRepository.class);

  private ViolationService violationService;

  @BeforeEach
  void setUp() {
    violationService =
        new ViolationService(violationRepository, reviewRepository, escalationRepository);
  }

  private static Violation persisted(UUID id, UUID organizationId) {
    return new Violation(
        id,
        organizationId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        null,
        "PROCTOR_ALERT",
        ViolationSeverity.MEDIUM,
        "alert",
        ViolationStatus.OPEN,
        "{}",
        Instant.now(),
        "telemetry-service",
        Instant.now(),
        Instant.now(),
        1);
  }

  @Test
  void recordInsertsViolation() {
    UUID organizationId = UUID.randomUUID();
    when(violationRepository.insert(any(Violation.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            violationService.record(
                organizationId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "PROCTOR_ALERT",
                ViolationSeverity.MEDIUM,
                "alert",
                "{}",
                Instant.now(),
                "telemetry-service"))
        .assertNext(
            violation -> {
              assertThat(violation.getRuleCode()).isEqualTo("PROCTOR_ALERT");
              assertThat(violation.getStatus()).isEqualTo(ViolationStatus.OPEN);
            })
        .verifyComplete();
  }

  @Test
  void getReturnsNotFoundForUnknownViolation() {
    UUID id = UUID.randomUUID();
    when(violationRepository.findById(id)).thenReturn(Mono.empty());

    StepVerifier.create(violationService.get(UUID.randomUUID(), id))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void getRejectsCrossTenantViolation() {
    UUID id = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    when(violationRepository.findById(id)).thenReturn(Mono.just(persisted(id, organizationId)));

    StepVerifier.create(violationService.get(UUID.randomUUID(), id))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void reviewConfirmResolvesViolation() {
    UUID id = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    when(violationRepository.findById(id)).thenReturn(Mono.just(persisted(id, organizationId)));
    when(reviewRepository.insert(any()))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(violationRepository.updateStatus(id, organizationId, ViolationStatus.RESOLVED))
        .thenReturn(Mono.just(persisted(id, organizationId)));

    StepVerifier.create(
            violationService.review(
                organizationId, id, UUID.randomUUID(), ReviewAction.CONFIRM, "confirmed", null))
        .assertNext(violation -> assertThat(violation.getStatus()).isEqualTo(ViolationStatus.OPEN))
        .verifyComplete();
    verify(violationRepository).updateStatus(id, organizationId, ViolationStatus.RESOLVED);
  }

  @Test
  void reviewDismissDismissesViolation() {
    UUID id = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    when(violationRepository.findById(id)).thenReturn(Mono.just(persisted(id, organizationId)));
    when(reviewRepository.insert(any()))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(violationRepository.updateStatus(id, organizationId, ViolationStatus.DISMISSED))
        .thenReturn(Mono.just(persisted(id, organizationId)));

    StepVerifier.create(
            violationService.review(
                organizationId, id, UUID.randomUUID(), ReviewAction.DISMISS, "no issue", null))
        .assertNext(violation -> assertThat(violation.getStatus()).isEqualTo(ViolationStatus.OPEN))
        .verifyComplete();
    verify(violationRepository).updateStatus(id, organizationId, ViolationStatus.DISMISSED);
  }

  @Test
  void reviewEscalateRequiresTargetReviewer() {
    UUID id = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    when(violationRepository.findById(id)).thenReturn(Mono.just(persisted(id, organizationId)));
    when(reviewRepository.insert(any()))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            violationService.review(
                organizationId, id, UUID.randomUUID(), ReviewAction.ESCALATE, "needs review", null))
        .expectError(ValidationFailedException.class)
        .verify();
  }

  @Test
  void reviewEscalateCreatesEscalation() {
    UUID id = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    when(violationRepository.findById(id)).thenReturn(Mono.just(persisted(id, organizationId)));
    when(reviewRepository.insert(any()))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(escalationRepository.insert(any())).thenReturn(Mono.empty());
    when(violationRepository.updateStatus(id, organizationId, ViolationStatus.ESCALATED))
        .thenReturn(Mono.just(persisted(id, organizationId)));

    StepVerifier.create(
            violationService.review(
                organizationId,
                id,
                UUID.randomUUID(),
                ReviewAction.ESCALATE,
                "needs review",
                UUID.randomUUID()))
        .assertNext(violation -> assertThat(violation.getStatus()).isEqualTo(ViolationStatus.OPEN))
        .verifyComplete();
    verify(escalationRepository).insert(any());
    verify(violationRepository).updateStatus(id, organizationId, ViolationStatus.ESCALATED);
  }

  @Test
  void listDelegatesToRepository() {
    UUID organizationId = UUID.randomUUID();
    when(violationRepository.listByOrganization(organizationId, null, null))
        .thenReturn(Flux.just(persisted(UUID.randomUUID(), organizationId)));

    StepVerifier.create(violationService.list(organizationId, null, null))
        .assertNext(violation -> assertThat(violation.getRuleCode()).isEqualTo("PROCTOR_ALERT"))
        .verifyComplete();
  }
}
