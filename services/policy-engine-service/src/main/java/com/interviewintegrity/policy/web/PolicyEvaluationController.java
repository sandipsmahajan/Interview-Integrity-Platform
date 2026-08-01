package com.interviewintegrity.policy.web;

import com.interviewintegrity.policy.domain.PolicyRule;
import com.interviewintegrity.policy.domain.Violation;
import com.interviewintegrity.policy.service.EvaluationEvent;
import com.interviewintegrity.policy.service.PolicyEvaluationService;
import com.interviewintegrity.policy.web.dto.EvaluateEventRequest;
import com.interviewintegrity.policy.web.dto.RuleResponse;
import com.interviewintegrity.policy.web.dto.ViolationResponse;
import com.interviewintegrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/** Policy evaluation endpoints. */
@RestController
@RequestMapping("/api/v1/policies/{policyId}")
@Tag(name = "Evaluation", description = "Evaluate telemetry events against policies")
public final class PolicyEvaluationController {

  private final PolicyEvaluationService evaluationService;

  /** Creates the controller bound to the evaluation service. */
  public PolicyEvaluationController(PolicyEvaluationService evaluationService) {
    this.evaluationService = evaluationService;
  }

  /** Returns the rules of a policy that match the given event. */
  @PostMapping("/evaluate")
  @Operation(summary = "Evaluate an event against a policy")
  public Flux<RuleResponse> evaluate(
      Authentication authentication,
      @PathVariable UUID policyId,
      @Valid @RequestBody EvaluateEventRequest request) {
    return evaluationService
        .evaluate(
            SecurityPrincipals.organizationId(authentication),
            policyId,
            new EvaluationEvent(request.eventType(), request.data()))
        .map(this::toRuleResponse);
  }

  /** Evaluates an event and records a violation for every matching rule. */
  @PostMapping("/violate")
  @Operation(summary = "Evaluate an event and record violations")
  public Flux<ViolationResponse> violate(
      Authentication authentication,
      @PathVariable UUID policyId,
      @Valid @RequestBody EvaluateEventRequest request) {
    return evaluationService
        .violate(
            SecurityPrincipals.organizationId(authentication),
            policyId,
            new EvaluationEvent(request.eventType(), request.data()),
            SecurityPrincipals.userId(authentication))
        .map(this::toViolationResponse);
  }

  private RuleResponse toRuleResponse(PolicyRule rule) {
    return new RuleResponse(
        rule.getId(),
        rule.getPolicyId(),
        rule.getRuleCode(),
        rule.getDescription(),
        rule.getCondition(),
        rule.getSeverity(),
        rule.getWeight(),
        rule.getOrderIndex(),
        rule.isEnabled(),
        rule.getCreatedAt(),
        rule.getUpdatedAt(),
        rule.getVersion());
  }

  private ViolationResponse toViolationResponse(Violation violation) {
    return new ViolationResponse(
        violation.getId(),
        violation.getSessionId(),
        violation.getInterviewId(),
        violation.getPolicyId(),
        violation.getRuleCode(),
        violation.getSeverity(),
        violation.getMessage(),
        violation.getStatus(),
        violation.getEvidence(),
        violation.getOccurredAt(),
        violation.getDetectedBy(),
        violation.getCreatedAt());
  }
}
