package com.interviewintegrity.policy.web;

import com.interviewintegrity.policy.domain.PolicyRule;
import com.interviewintegrity.policy.service.PolicyRuleService;
import com.interviewintegrity.policy.web.dto.CreateRuleRequest;
import com.interviewintegrity.policy.web.dto.RuleResponse;
import com.interviewintegrity.policy.web.dto.UpdateRuleRequest;
import com.interviewintegrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Policy rule endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/policies/{policyId}/rules")
@Tag(name = "Rules", description = "Manage evaluable policy rules")
public final class PolicyRuleController {

  private final PolicyRuleService ruleService;

  /** Creates the controller bound to the rule service. */
  public PolicyRuleController(PolicyRuleService ruleService) {
    this.ruleService = ruleService;
  }

  /** Adds a rule to a policy. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a policy rule")
  public Mono<RuleResponse> create(
      Authentication authentication,
      @PathVariable UUID policyId,
      @Valid @RequestBody CreateRuleRequest request) {
    return ruleService
        .create(
            SecurityPrincipals.organizationId(authentication),
            policyId,
            request.ruleCode(),
            request.description(),
            request.condition(),
            request.severity(),
            request.weight(),
            request.orderIndex(),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  /** Lists the rules of a policy. */
  @GetMapping
  @Operation(summary = "List policy rules")
  public Flux<RuleResponse> list(Authentication authentication, @PathVariable UUID policyId) {
    return ruleService
        .list(SecurityPrincipals.organizationId(authentication), policyId)
        .map(this::toResponse);
  }

  /** Updates the editable attributes of a rule. */
  @PutMapping("/{ruleId}")
  @Operation(summary = "Update a policy rule")
  public Mono<RuleResponse> update(
      Authentication authentication,
      @PathVariable UUID policyId,
      @PathVariable UUID ruleId,
      @Valid @RequestBody UpdateRuleRequest request) {
    return ruleService
        .update(
            SecurityPrincipals.organizationId(authentication),
            ruleId,
            request.ruleCode(),
            request.description(),
            request.condition(),
            request.severity(),
            request.weight(),
            request.orderIndex(),
            request.enabled(),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  /** Soft deletes a rule. */
  @DeleteMapping("/{ruleId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a policy rule")
  public Mono<Void> delete(
      Authentication authentication, @PathVariable UUID policyId, @PathVariable UUID ruleId) {
    return ruleService.delete(
        SecurityPrincipals.organizationId(authentication),
        ruleId,
        SecurityPrincipals.userId(authentication));
  }

  private RuleResponse toResponse(PolicyRule rule) {
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
}
