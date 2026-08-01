package com.interviewintegrity.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.policy.domain.Policy;
import com.interviewintegrity.policy.domain.PolicyRule;
import com.interviewintegrity.policy.domain.ViolationSeverity;
import com.interviewintegrity.policy.repository.PolicyRuleRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the policy rule service. */
class PolicyRuleServiceTest {

  private final PolicyRuleRepository ruleRepository = Mockito.mock(PolicyRuleRepository.class);
  private final PolicyService policyService = Mockito.mock(PolicyService.class);

  private PolicyRuleService ruleService;

  @BeforeEach
  void setUp() {
    ruleService = new PolicyRuleService(ruleRepository, policyService);
  }

  private static Policy policy(UUID id, UUID organizationId) {
    return new Policy(
        id,
        organizationId,
        "NO_COPY",
        "No Copy",
        null,
        com.interviewintegrity.policy.domain.PolicyStatus.ACTIVE,
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

  private static PolicyRule persisted(UUID id, UUID policyId, UUID organizationId) {
    return new PolicyRule(
        id,
        organizationId,
        policyId,
        "NO_KEYS",
        null,
        "{\"eventType\":\"KEYSTROKE\"}",
        ViolationSeverity.HIGH,
        1,
        0,
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
  void createRequiresExistingPolicy() {
    UUID organizationId = UUID.randomUUID();
    UUID policyId = UUID.randomUUID();
    when(policyService.get(organizationId, policyId))
        .thenReturn(Mono.just(policy(policyId, organizationId)));
    when(ruleRepository.insert(any(PolicyRule.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            ruleService.create(
                organizationId,
                policyId,
                "NO_KEYS",
                null,
                "{\"eventType\":\"KEYSTROKE\"}",
                ViolationSeverity.HIGH,
                null,
                null,
                UUID.randomUUID()))
        .assertNext(
            rule -> {
              assertThat(rule.getRuleCode()).isEqualTo("NO_KEYS");
              assertThat(rule.getSeverity()).isEqualTo(ViolationSeverity.HIGH);
            })
        .verifyComplete();
  }

  @Test
  void getReturnsNotFoundForUnknownRule() {
    UUID id = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    when(ruleRepository.findById(id, organizationId)).thenReturn(Mono.empty());

    StepVerifier.create(ruleService.get(organizationId, id))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void listReturnsRulesOfPolicy() {
    UUID organizationId = UUID.randomUUID();
    UUID policyId = UUID.randomUUID();
    when(policyService.get(organizationId, policyId))
        .thenReturn(Mono.just(policy(policyId, organizationId)));
    when(ruleRepository.listByPolicy(policyId))
        .thenReturn(Flux.just(persisted(UUID.randomUUID(), policyId, organizationId)));

    StepVerifier.create(ruleService.list(organizationId, policyId))
        .assertNext(rule -> assertThat(rule.getRuleCode()).isEqualTo("NO_KEYS"))
        .verifyComplete();
  }

  @Test
  void updateMutatesRule() {
    UUID id = UUID.randomUUID();
    UUID policyId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    PolicyRule rule = persisted(id, policyId, organizationId);
    when(ruleRepository.findById(id, organizationId)).thenReturn(Mono.just(rule));
    when(ruleRepository.update(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Mono.just(rule));

    StepVerifier.create(
            ruleService.update(
                organizationId,
                id,
                "NO_KEYS",
                null,
                "{\"eventType\":\"COPY\"}",
                ViolationSeverity.CRITICAL,
                5,
                1,
                true,
                UUID.randomUUID()))
        .assertNext(updated -> assertThat(updated.getRuleCode()).isEqualTo("NO_KEYS"))
        .verifyComplete();
    verify(ruleRepository)
        .update(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void deleteSoftDeletesRule() {
    UUID id = UUID.randomUUID();
    UUID policyId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    when(ruleRepository.findById(id, organizationId))
        .thenReturn(Mono.just(persisted(id, policyId, organizationId)));
    when(ruleRepository.softDelete(any(), any(), any())).thenReturn(Mono.empty());

    StepVerifier.create(ruleService.delete(organizationId, id, UUID.randomUUID())).verifyComplete();
    verify(ruleRepository).softDelete(any(), any(), any());
  }
}
