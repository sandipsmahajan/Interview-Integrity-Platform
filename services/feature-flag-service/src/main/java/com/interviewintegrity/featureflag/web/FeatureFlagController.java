package com.interviewintegrity.featureflag.web;

import com.interviewintegrity.featureflag.domain.FeatureFlag;
import com.interviewintegrity.featureflag.domain.FeatureFlagHistory;
import com.interviewintegrity.featureflag.domain.FlagTarget;
import com.interviewintegrity.featureflag.service.FeatureFlagService;
import com.interviewintegrity.featureflag.web.dto.AddTargetRequest;
import com.interviewintegrity.featureflag.web.dto.CreateFlagRequest;
import com.interviewintegrity.featureflag.web.dto.FeatureFlagHistoryResponse;
import com.interviewintegrity.featureflag.web.dto.FeatureFlagResponse;
import com.interviewintegrity.featureflag.web.dto.FlagTargetResponse;
import com.interviewintegrity.featureflag.web.dto.UpdateFlagRequest;
import com.interviewintegrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Feature flag configuration and targeting endpoints. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Feature Flags", description = "Manage flag configurations and rollouts")
public final class FeatureFlagController {

  private static final String DEFAULT_ENVIRONMENT = "PRODUCTION";

  private final FeatureFlagService flagService;

  /** Creates the controller bound to the flag service. */
  public FeatureFlagController(FeatureFlagService flagService) {
    this.flagService = flagService;
  }

  /** Creates a flag for a feature. */
  @PostMapping("/features/{featureId}/flags")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a feature flag")
  public Mono<FeatureFlagResponse> createFlag(
      Authentication authentication,
      @PathVariable UUID featureId,
      @Valid @RequestBody CreateFlagRequest request) {
    return flagService
        .createFlag(
            SecurityPrincipals.organizationId(authentication),
            featureId,
            environment(request.environment()),
            request.enabled(),
            request.rolloutPercent(),
            request.defaultVariant(),
            request.variants(),
            request.rules(),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  /** Lists the flags of a feature. */
  @GetMapping("/features/{featureId}/flags")
  @Operation(summary = "List feature flags")
  public Flux<FeatureFlagResponse> listFlags(
      Authentication authentication, @PathVariable UUID featureId) {
    return flagService
        .listFlags(SecurityPrincipals.organizationId(authentication), featureId)
        .map(this::toResponse);
  }

  /** Returns a single flag. */
  @GetMapping("/flags/{flagId}")
  @Operation(summary = "Get a feature flag")
  public Mono<FeatureFlagResponse> getFlag(
      Authentication authentication, @PathVariable UUID flagId) {
    return flagService
        .getFlag(flagId, SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  /** Updates a flag configuration. */
  @PatchMapping("/flags/{flagId}")
  @Operation(summary = "Update a feature flag")
  public Mono<FeatureFlagResponse> updateFlag(
      Authentication authentication,
      @PathVariable UUID flagId,
      @Valid @RequestBody UpdateFlagRequest request) {
    return flagService
        .updateFlag(
            flagId,
            SecurityPrincipals.organizationId(authentication),
            request.enabled(),
            request.rolloutPercent(),
            request.defaultVariant(),
            request.variants(),
            request.rules(),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  /** Lists the per-user overrides of a flag. */
  @GetMapping("/flags/{flagId}/targets")
  @Operation(summary = "List flag targets")
  public Flux<FlagTargetResponse> listTargets(
      Authentication authentication, @PathVariable UUID flagId) {
    return flagService
        .listTargets(flagId, SecurityPrincipals.organizationId(authentication))
        .map(this::toTargetResponse);
  }

  /** Adds or replaces a per-user override for a flag. */
  @PostMapping("/flags/{flagId}/targets")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Add a flag target")
  public Mono<FlagTargetResponse> addTarget(
      Authentication authentication,
      @PathVariable UUID flagId,
      @Valid @RequestBody AddTargetRequest request) {
    return flagService
        .addTarget(
            flagId,
            SecurityPrincipals.organizationId(authentication),
            request.userId(),
            request.variant(),
            request.enabled(),
            SecurityPrincipals.userId(authentication))
        .map(this::toTargetResponse);
  }

  /** Removes a per-user override for a flag. */
  @DeleteMapping("/flags/{flagId}/targets/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Remove a flag target")
  public Mono<Void> removeTarget(
      Authentication authentication, @PathVariable UUID flagId, @PathVariable UUID userId) {
    return flagService.removeTarget(
        flagId, SecurityPrincipals.organizationId(authentication), userId);
  }

  /** Lists the history snapshots of a flag. */
  @GetMapping("/flags/{flagId}/history")
  @Operation(summary = "Get flag history")
  public Flux<FeatureFlagHistoryResponse> history(
      Authentication authentication, @PathVariable UUID flagId) {
    return flagService
        .history(flagId, SecurityPrincipals.organizationId(authentication))
        .map(this::toHistoryResponse);
  }

  private String environment(String environment) {
    if (environment == null || environment.isBlank()) {
      return DEFAULT_ENVIRONMENT;
    }
    return environment.trim().toUpperCase(Locale.ROOT);
  }

  private FeatureFlagResponse toResponse(FeatureFlag flag) {
    return new FeatureFlagResponse(
        flag.getId(),
        flag.getOrganizationId(),
        flag.getFeatureId(),
        flag.getEnvironment(),
        flag.isEnabled(),
        flag.getRolloutPercent(),
        flag.getDefaultVariant(),
        flag.getVariants(),
        flag.getRules(),
        flag.getCreatedAt(),
        flag.getUpdatedAt());
  }

  private FlagTargetResponse toTargetResponse(FlagTarget target) {
    return new FlagTargetResponse(
        target.getFlagId(),
        target.getUserId(),
        target.getVariant(),
        target.isEnabled(),
        target.getAddedAt());
  }

  private FeatureFlagHistoryResponse toHistoryResponse(FeatureFlagHistory history) {
    return new FeatureFlagHistoryResponse(
        history.getHistoryId(),
        history.getHistoryAction(),
        history.getChangedBy(),
        history.getChangedAt(),
        history.getId(),
        history.getEnvironment(),
        history.isEnabled(),
        history.getRolloutPercent(),
        history.getDefaultVariant(),
        history.getVariants(),
        history.getRules(),
        history.getVersion());
  }
}
