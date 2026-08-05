package com.integrity.policy.web;

import com.integrity.policy.service.PolicyMapper;
import com.integrity.policy.service.PolicyRuleService;
import com.integrity.policy.web.dto.CreateRuleRequest;
import com.integrity.policy.web.dto.RuleResponse;
import com.integrity.policy.web.dto.UpdateRuleRequest;
import com.integrity.security.SecurityPrincipals;
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
  private final PolicyMapper mapper;

  /** Creates the controller bound to the rule service and mapper. */
  public PolicyRuleController(PolicyRuleService ruleService, PolicyMapper mapper) {
    this.ruleService = ruleService;
    this.mapper = mapper;
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
        .map(mapper::toResponse);
  }

  /** Lists the rules of a policy. */
  @GetMapping
  @Operation(summary = "List policy rules")
  public Flux<RuleResponse> list(Authentication authentication, @PathVariable UUID policyId) {
    return ruleService
        .list(SecurityPrincipals.organizationId(authentication), policyId)
        .map(mapper::toResponse);
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
        .map(mapper::toResponse);
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
}
