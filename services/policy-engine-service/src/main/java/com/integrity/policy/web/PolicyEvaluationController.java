package com.integrity.policy.web;

import com.integrity.policy.service.EvaluationEvent;
import com.integrity.policy.service.PolicyEvaluationService;
import com.integrity.policy.service.PolicyMapper;
import com.integrity.policy.web.dto.EvaluateEventRequest;
import com.integrity.policy.web.dto.RuleResponse;
import com.integrity.policy.web.dto.ViolationResponse;
import com.integrity.security.SecurityPrincipals;
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
  private final PolicyMapper mapper;

  /** Creates the controller bound to the evaluation service and mapper. */
  public PolicyEvaluationController(
      PolicyEvaluationService evaluationService, PolicyMapper mapper) {
    this.evaluationService = evaluationService;
    this.mapper = mapper;
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
        .map(mapper::toResponse);
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
        .map(mapper::toResponse);
  }
}
