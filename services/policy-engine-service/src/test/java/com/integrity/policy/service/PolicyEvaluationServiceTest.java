package com.integrity.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.integrity.policy.domain.PolicyRule;
import com.integrity.policy.domain.Violation;
import com.integrity.policy.domain.ViolationSeverity;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the deterministic rule evaluator. */
class PolicyEvaluationServiceTest {

  private final PolicyRuleService ruleService = Mockito.mock(PolicyRuleService.class);
  private final ViolationService violationService = Mockito.mock(ViolationService.class);

  private PolicyEvaluationService evaluationService;

  @BeforeEach
  void setUp() {
    evaluationService = new PolicyEvaluationService(ruleService, violationService);
  }

  private static PolicyRule rule(String condition, ViolationSeverity severity) {
    return new PolicyRule(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "HEARTBEAT_GAP",
        null,
        condition,
        severity,
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
  void matchesGtComparison() {
    String condition = "{\"eventType\":\"HEARTBEAT\",\"op\":\"gt\",\"field\":\"gap\",\"value\":10}";
    EvaluationEvent matching = new EvaluationEvent("HEARTBEAT", Map.of("gap", 15));
    EvaluationEvent notMatching = new EvaluationEvent("HEARTBEAT", Map.of("gap", 5));

    assertThat(evaluationService.matches(condition, matching)).isTrue();
    assertThat(evaluationService.matches(condition, notMatching)).isFalse();
  }

  @Test
  void matchesRejectsWrongEventType() {
    String condition = "{\"eventType\":\"HEARTBEAT\",\"op\":\"eq\",\"field\":\"gap\",\"value\":1}";
    EvaluationEvent other = new EvaluationEvent("KEYSTROKE", Map.of("gap", 1));

    assertThat(evaluationService.matches(condition, other)).isFalse();
  }

  @Test
  void evaluateReturnsOnlyMatchingEnabledRules() {
    UUID organizationId = UUID.randomUUID();
    UUID policyId = UUID.randomUUID();
    String condition = "{\"eventType\":\"HEARTBEAT\",\"op\":\"gt\",\"field\":\"gap\",\"value\":10}";
    PolicyRule matchingRule = rule(condition, ViolationSeverity.HIGH);
    when(ruleService.list(organizationId, policyId)).thenReturn(Flux.just(matchingRule));

    StepVerifier.create(
            evaluationService.evaluate(
                organizationId, policyId, new EvaluationEvent("HEARTBEAT", Map.of("gap", 20))))
        .assertNext(
            matched -> {
              assertThat(matched.getRuleCode()).isEqualTo("HEARTBEAT_GAP");
              assertThat(matched.getSeverity()).isEqualTo(ViolationSeverity.HIGH);
            })
        .verifyComplete();
  }

  @Test
  void evaluateIgnoresNonMatchingRules() {
    UUID organizationId = UUID.randomUUID();
    UUID policyId = UUID.randomUUID();
    String condition = "{\"eventType\":\"HEARTBEAT\",\"op\":\"gt\",\"field\":\"gap\",\"value\":10}";
    PolicyRule rule = rule(condition, ViolationSeverity.HIGH);
    when(ruleService.list(organizationId, policyId)).thenReturn(Flux.just(rule));

    StepVerifier.create(
            evaluationService.evaluate(
                organizationId, policyId, new EvaluationEvent("HEARTBEAT", Map.of("gap", 3))))
        .verifyComplete();
  }

  @Test
  void violateRecordsViolationForMatchingRule() {
    UUID organizationId = UUID.randomUUID();
    UUID policyId = UUID.randomUUID();
    String condition = "{\"eventType\":\"HEARTBEAT\",\"op\":\"gt\",\"field\":\"gap\",\"value\":10}";
    PolicyRule matchingRule = rule(condition, ViolationSeverity.HIGH);
    when(ruleService.list(organizationId, policyId)).thenReturn(Flux.just(matchingRule));
    when(violationService.record(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(
            Mono.just(
                new Violation(
                    organizationId,
                    UUID.randomUUID(),
                    null,
                    policyId,
                    "HEARTBEAT_GAP",
                    ViolationSeverity.HIGH,
                    "matched",
                    "{}",
                    Instant.now(),
                    "policy-engine-service")));

    StepVerifier.create(
            evaluationService.violate(
                organizationId,
                policyId,
                new EvaluationEvent("HEARTBEAT", Map.of("gap", 20)),
                null))
        .assertNext(violation -> assertThat(violation.getRuleCode()).isEqualTo("HEARTBEAT_GAP"))
        .verifyComplete();
    verify(violationService)
        .record(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
  }
}
