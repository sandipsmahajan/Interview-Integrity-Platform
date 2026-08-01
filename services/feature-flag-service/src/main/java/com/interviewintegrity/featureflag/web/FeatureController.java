package com.interviewintegrity.featureflag.web;

import com.interviewintegrity.featureflag.domain.Feature;
import com.interviewintegrity.featureflag.service.FeatureService;
import com.interviewintegrity.featureflag.web.dto.CreateFeatureRequest;
import com.interviewintegrity.featureflag.web.dto.FeatureResponse;
import com.interviewintegrity.featureflag.web.dto.UpdateFeatureRequest;
import com.interviewintegrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

/** Feature catalog endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/features")
@Tag(name = "Features", description = "Manage the feature catalog")
public final class FeatureController {

  private final FeatureService featureService;

  /** Creates the controller bound to the feature service. */
  public FeatureController(FeatureService featureService) {
    this.featureService = featureService;
  }

  /** Creates a feature. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a feature")
  public Mono<FeatureResponse> create(
      Authentication authentication, @Valid @RequestBody CreateFeatureRequest request) {
    return featureService
        .create(
            SecurityPrincipals.organizationId(authentication),
            request.code().trim(),
            request.name().trim(),
            request.description(),
            request.kind(),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  /** Lists the features of the organization. */
  @GetMapping
  @Operation(summary = "List features")
  public Flux<FeatureResponse> list(Authentication authentication) {
    return featureService
        .list(SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  /** Returns a single feature. */
  @GetMapping("/{featureId}")
  @Operation(summary = "Get a feature")
  public Mono<FeatureResponse> get(Authentication authentication, @PathVariable UUID featureId) {
    return featureService
        .get(featureId, SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  /** Updates a feature. */
  @PatchMapping("/{featureId}")
  @Operation(summary = "Update a feature")
  public Mono<FeatureResponse> update(
      Authentication authentication,
      @PathVariable UUID featureId,
      @Valid @RequestBody UpdateFeatureRequest request) {
    return featureService
        .update(
            featureId,
            SecurityPrincipals.organizationId(authentication),
            request.name().trim(),
            request.description(),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  /** Soft deletes a feature. */
  @DeleteMapping("/{featureId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a feature")
  public Mono<Void> delete(Authentication authentication, @PathVariable UUID featureId) {
    return featureService.delete(
        featureId,
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication));
  }

  private FeatureResponse toResponse(Feature feature) {
    return new FeatureResponse(
        feature.getId(),
        feature.getOrganizationId(),
        feature.getCode(),
        feature.getName(),
        feature.getDescription(),
        feature.getKind(),
        feature.getCreatedAt());
  }
}
