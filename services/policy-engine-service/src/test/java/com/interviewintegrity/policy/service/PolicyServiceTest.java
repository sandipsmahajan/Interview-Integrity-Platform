package com.interviewintegrity.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.policy.domain.Policy;
import com.interviewintegrity.policy.domain.PolicyStatus;
import com.interviewintegrity.policy.domain.ViolationSeverity;
import com.interviewintegrity.policy.repository.PolicyRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the policy service. */
class PolicyServiceTest {

  private final PolicyRepository policyRepository = Mockito.mock(PolicyRepository.class);
  private final PolicyPublishingService publishingService =
      Mockito.mock(PolicyPublishingService.class);

  private PolicyService policyService;

  @BeforeEach
  void setUp() {
    policyService = new PolicyService(policyRepository, publishingService);
  }

  private static Policy persisted(UUID id, UUID organizationId) {
    return new Policy(
        id,
        organizationId,
        "NO_COPY",
        "No Copy",
        null,
        PolicyStatus.DRAFT,
        ViolationSeverity.MEDIUM,
        100,
        true,
        null,
        Instant.now(),
        null,
        Instant.now(),
        null,
        null,
        1);
  }

  @Test
  void createInsertsDraftPolicy() {
    UUID organizationId = UUID.randomUUID();
    when(policyRepository.insert(any(Policy.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            policyService.create(
                organizationId, "NO_COPY", "No Copy", null, null, null, UUID.randomUUID()))
        .assertNext(
            policy -> {
              assertThat(policy.getCode()).isEqualTo("NO_COPY");
              assertThat(policy.getStatus()).isEqualTo(PolicyStatus.DRAFT);
            })
        .verifyComplete();
  }

  @Test
  void getReturnsNotFoundForUnknownPolicy() {
    UUID id = UUID.randomUUID();
    when(policyRepository.findById(id)).thenReturn(Mono.empty());

    StepVerifier.create(policyService.get(UUID.randomUUID(), id))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void getRejectsCrossTenantPolicy() {
    UUID id = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    when(policyRepository.findById(id)).thenReturn(Mono.just(persisted(id, organizationId)));

    StepVerifier.create(policyService.get(UUID.randomUUID(), id))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void updatePreservesCurrentStatus() {
    UUID id = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    Policy policy = persisted(id, organizationId);
    when(policyRepository.findById(id)).thenReturn(Mono.just(policy));
    when(policyRepository.update(
            eq(id), eq(organizationId), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Mono.just(policy));

    StepVerifier.create(
            policyService.update(
                organizationId,
                id,
                "New Name",
                null,
                ViolationSeverity.HIGH,
                50,
                true,
                UUID.randomUUID()))
        .assertNext(updated -> assertThat(updated.getCode()).isEqualTo("NO_COPY"))
        .verifyComplete();
  }

  @Test
  void changeStatusToActivePublishesVersion() {
    UUID id = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    Policy policy = persisted(id, organizationId);
    when(policyRepository.findById(id)).thenReturn(Mono.just(policy));
    when(policyRepository.changeStatus(eq(id), eq(organizationId), any(), any()))
        .thenReturn(Mono.just(policy));
    when(publishingService.publishVersion(any(), any())).thenReturn(Mono.empty());

    StepVerifier.create(
            policyService.changeStatus(organizationId, id, PolicyStatus.ACTIVE, UUID.randomUUID()))
        .assertNext(updated -> assertThat(updated.getStatus()).isEqualTo(PolicyStatus.DRAFT))
        .verifyComplete();
    verify(publishingService).publishVersion(any(), any());
  }

  @Test
  void deleteSoftDeletesPolicy() {
    UUID id = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    when(policyRepository.findById(id)).thenReturn(Mono.just(persisted(id, organizationId)));
    when(policyRepository.softDelete(eq(id), eq(organizationId), any())).thenReturn(Mono.empty());

    StepVerifier.create(policyService.delete(organizationId, id, UUID.randomUUID()))
        .verifyComplete();
    verify(policyRepository).softDelete(eq(id), eq(organizationId), any());
  }

  @Test
  void listDelegatesToRepository() {
    UUID organizationId = UUID.randomUUID();
    when(policyRepository.listByOrganization(organizationId))
        .thenReturn(Flux.just(persisted(UUID.randomUUID(), organizationId)));

    StepVerifier.create(policyService.list(organizationId))
        .assertNext(policy -> assertThat(policy.getCode()).isEqualTo("NO_COPY"))
        .verifyComplete();
  }
}
