package com.integrity.policy.web;

import com.integrity.policy.domain.ViolationSeverity;
import com.integrity.policy.domain.ViolationStatus;
import com.integrity.policy.service.PolicyMapper;
import com.integrity.policy.service.ViolationService;
import com.integrity.policy.web.dto.ReviewViolationRequest;
import com.integrity.policy.web.dto.ViolationResponse;
import com.integrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Violation triage endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/violations")
@Tag(name = "Violations", description = "Triage detected integrity violations")
public final class ViolationController {

  private final ViolationService violationService;
  private final PolicyMapper mapper;

  /** Creates the controller bound to the violation service and mapper. */
  public ViolationController(ViolationService violationService, PolicyMapper mapper) {
    this.violationService = violationService;
    this.mapper = mapper;
  }

  /** Lists the violations of the organization with optional filters. */
  @GetMapping
  @Operation(summary = "List violations")
  public Flux<ViolationResponse> list(
      Authentication authentication,
      @RequestParam(required = false) ViolationStatus status,
      @RequestParam(required = false) ViolationSeverity severity) {
    return violationService
        .list(SecurityPrincipals.organizationId(authentication), status, severity)
        .map(mapper::toResponse);
  }

  /** Returns a single violation. */
  @GetMapping("/{violationId}")
  @Operation(summary = "Get a violation")
  public Mono<ViolationResponse> get(
      Authentication authentication, @PathVariable UUID violationId) {
    return violationService
        .get(SecurityPrincipals.organizationId(authentication), violationId)
        .map(mapper::toResponse);
  }

  /** Records a human review decision on a violation. */
  @PostMapping("/{violationId}/review")
  @Operation(summary = "Review a violation")
  public Mono<ViolationResponse> review(
      Authentication authentication,
      @PathVariable UUID violationId,
      @Valid @RequestBody ReviewViolationRequest request) {
    return violationService
        .review(
            SecurityPrincipals.organizationId(authentication),
            violationId,
            SecurityPrincipals.userId(authentication),
            request.action(),
            request.comment(),
            request.escalatedTo())
        .map(mapper::toResponse);
  }
}
