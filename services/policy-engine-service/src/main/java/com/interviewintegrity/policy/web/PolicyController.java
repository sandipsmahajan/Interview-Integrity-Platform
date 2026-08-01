package com.interviewintegrity.policy.web;

import com.interviewintegrity.policy.domain.Policy;
import com.interviewintegrity.policy.service.PolicyService;
import com.interviewintegrity.policy.web.dto.ChangePolicyStatusRequest;
import com.interviewintegrity.policy.web.dto.CreatePolicyRequest;
import com.interviewintegrity.policy.web.dto.PolicyResponse;
import com.interviewintegrity.policy.web.dto.UpdatePolicyRequest;
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

/** Policy catalog endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/policies")
@Tag(name = "Policies", description = "Manage integrity policies")
public final class PolicyController {

  private final PolicyService policyService;

  /** Creates the controller bound to the policy service. */
  public PolicyController(PolicyService policyService) {
    this.policyService = policyService;
  }

  /** Creates a new draft policy. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a policy")
  public Mono<PolicyResponse> create(
      Authentication authentication, @Valid @RequestBody CreatePolicyRequest request) {
    return policyService
        .create(
            SecurityPrincipals.organizationId(authentication),
            request.code(),
            request.name(),
            request.description(),
            request.defaultSeverity(),
            request.priority(),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  /** Lists the policies of the organization. */
  @GetMapping
  @Operation(summary = "List policies")
  public Flux<PolicyResponse> list(Authentication authentication) {
    return policyService
        .list(SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  /** Returns a single policy. */
  @GetMapping("/{policyId}")
  @Operation(summary = "Get a policy")
  public Mono<PolicyResponse> get(Authentication authentication, @PathVariable UUID policyId) {
    return policyService
        .get(SecurityPrincipals.organizationId(authentication), policyId)
        .map(this::toResponse);
  }

  /** Updates the editable attributes of a policy. */
  @PutMapping("/{policyId}")
  @Operation(summary = "Update a policy")
  public Mono<PolicyResponse> update(
      Authentication authentication,
      @PathVariable UUID policyId,
      @Valid @RequestBody UpdatePolicyRequest request) {
    return policyService
        .update(
            SecurityPrincipals.organizationId(authentication),
            policyId,
            request.name(),
            request.description(),
            request.defaultSeverity(),
            request.priority(),
            request.enabled(),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  /** Transitions a policy lifecycle state. */
  @PostMapping("/{policyId}/status")
  @Operation(summary = "Change policy status")
  public Mono<PolicyResponse> changeStatus(
      Authentication authentication,
      @PathVariable UUID policyId,
      @Valid @RequestBody ChangePolicyStatusRequest request) {
    return policyService
        .changeStatus(
            SecurityPrincipals.organizationId(authentication),
            policyId,
            request.status(),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  /** Soft deletes a policy. */
  @DeleteMapping("/{policyId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a policy")
  public Mono<Void> delete(Authentication authentication, @PathVariable UUID policyId) {
    return policyService.delete(
        SecurityPrincipals.organizationId(authentication),
        policyId,
        SecurityPrincipals.userId(authentication));
  }

  private PolicyResponse toResponse(Policy policy) {
    return new PolicyResponse(
        policy.getId(),
        policy.getOrganizationId(),
        policy.getCode(),
        policy.getName(),
        policy.getDescription(),
        policy.getStatus(),
        policy.getDefaultSeverity(),
        policy.getPriority(),
        policy.isEnabled(),
        policy.getCreatedAt(),
        policy.getUpdatedAt(),
        policy.getVersion());
  }
}
