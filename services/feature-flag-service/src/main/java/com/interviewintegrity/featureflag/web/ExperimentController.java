package com.interviewintegrity.featureflag.web;

import com.interviewintegrity.featureflag.domain.ExperimentStatus;
import com.interviewintegrity.featureflag.service.ExperimentService;
import com.interviewintegrity.featureflag.service.FeatureFlagMapper;
import com.interviewintegrity.featureflag.web.dto.CreateExperimentRequest;
import com.interviewintegrity.featureflag.web.dto.ExperimentResponse;
import com.interviewintegrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Experiment management endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/experiments")
@Tag(name = "Experiments", description = "Manage A/B experiments")
public final class ExperimentController {

  private final ExperimentService experimentService;
  private final FeatureFlagMapper mapper;

  /** Creates the controller bound to the experiment service and mapper. */
  public ExperimentController(ExperimentService experimentService, FeatureFlagMapper mapper) {
    this.experimentService = experimentService;
    this.mapper = mapper;
  }

  /** Creates a draft experiment. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create an experiment")
  public Mono<ExperimentResponse> create(
      Authentication authentication, @Valid @RequestBody CreateExperimentRequest request) {
    return experimentService
        .create(
            SecurityPrincipals.organizationId(authentication),
            request.name().trim(),
            request.featureId(),
            request.controlVariant(),
            request.treatmentVariant(),
            request.metrics(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Lists the experiments of the organization, optionally filtered by status. */
  @GetMapping
  @Operation(summary = "List experiments")
  public Flux<ExperimentResponse> list(
      Authentication authentication, @RequestParam(required = false) ExperimentStatus status) {
    return experimentService
        .list(SecurityPrincipals.organizationId(authentication), status)
        .map(mapper::toResponse);
  }

  /** Returns a single experiment. */
  @GetMapping("/{experimentId}")
  @Operation(summary = "Get an experiment")
  public Mono<ExperimentResponse> get(
      Authentication authentication, @PathVariable UUID experimentId) {
    return experimentService
        .get(experimentId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Starts the experiment. */
  @PostMapping("/{experimentId}/start")
  @Operation(summary = "Start an experiment")
  public Mono<ExperimentResponse> start(
      Authentication authentication, @PathVariable UUID experimentId) {
    return experimentService
        .start(
            experimentId,
            SecurityPrincipals.organizationId(authentication),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Pauses the experiment. */
  @PostMapping("/{experimentId}/pause")
  @Operation(summary = "Pause an experiment")
  public Mono<ExperimentResponse> pause(
      Authentication authentication, @PathVariable UUID experimentId) {
    return experimentService
        .pause(
            experimentId,
            SecurityPrincipals.organizationId(authentication),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Resumes the experiment. */
  @PostMapping("/{experimentId}/resume")
  @Operation(summary = "Resume an experiment")
  public Mono<ExperimentResponse> resume(
      Authentication authentication, @PathVariable UUID experimentId) {
    return experimentService
        .resume(
            experimentId,
            SecurityPrincipals.organizationId(authentication),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Completes the experiment. */
  @PostMapping("/{experimentId}/complete")
  @Operation(summary = "Complete an experiment")
  public Mono<ExperimentResponse> complete(
      Authentication authentication, @PathVariable UUID experimentId) {
    return experimentService
        .complete(
            experimentId,
            SecurityPrincipals.organizationId(authentication),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Rejects the experiment. */
  @PostMapping("/{experimentId}/reject")
  @Operation(summary = "Reject an experiment")
  public Mono<ExperimentResponse> reject(
      Authentication authentication, @PathVariable UUID experimentId) {
    return experimentService
        .reject(
            experimentId,
            SecurityPrincipals.organizationId(authentication),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }
}
