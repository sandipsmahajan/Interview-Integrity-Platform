package com.integrity.recruiter.web;

import com.integrity.recruiter.service.PipelineService;
import com.integrity.recruiter.service.RecruiterMapper;
import com.integrity.recruiter.web.dto.CandidatePipelineResponse;
import com.integrity.recruiter.web.dto.CreatePipelineStageRequest;
import com.integrity.recruiter.web.dto.PipelineStageResponse;
import com.integrity.recruiter.web.dto.UpdatePipelineStageRequest;
import com.integrity.security.SecurityPrincipals;
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

/** Pipeline stage management endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/stages")
@Tag(name = "Pipeline Stages", description = "Manage hiring pipeline stages")
public final class PipelineStageController {

  private final PipelineService pipelineService;
  private final RecruiterMapper mapper;

  /** Creates the controller bound to the pipeline service and mapper. */
  public PipelineStageController(PipelineService pipelineService, RecruiterMapper mapper) {
    this.pipelineService = pipelineService;
    this.mapper = mapper;
  }

  /** Creates a pipeline stage. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a pipeline stage")
  public Mono<PipelineStageResponse> create(
      Authentication authentication, @Valid @RequestBody CreatePipelineStageRequest request) {
    return pipelineService
        .createStage(
            SecurityPrincipals.organizationId(authentication),
            request.code().trim(),
            request.name().trim(),
            request.orderIndex(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Lists the stages of the organization. */
  @GetMapping
  @Operation(summary = "List pipeline stages")
  public Flux<PipelineStageResponse> list(Authentication authentication) {
    return pipelineService
        .listStages(SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Updates a stage. */
  @PatchMapping("/{stageId}")
  @Operation(summary = "Update a pipeline stage")
  public Mono<PipelineStageResponse> update(
      Authentication authentication,
      @PathVariable UUID stageId,
      @Valid @RequestBody UpdatePipelineStageRequest request) {
    return pipelineService
        .updateStage(
            stageId,
            SecurityPrincipals.organizationId(authentication),
            request.name().trim(),
            request.orderIndex(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Soft deletes a stage. */
  @DeleteMapping("/{stageId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a pipeline stage")
  public Mono<Void> delete(Authentication authentication, @PathVariable UUID stageId) {
    return pipelineService.deleteStage(
        stageId,
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication));
  }

  /** Lists the candidates currently in a stage. */
  @GetMapping("/{stageId}/candidates")
  @Operation(summary = "List candidates in a stage")
  public Flux<CandidatePipelineResponse> candidates(
      Authentication authentication, @PathVariable UUID stageId) {
    return pipelineService
        .listStageCandidates(SecurityPrincipals.organizationId(authentication), stageId)
        .map(mapper::toResponse);
  }
}
