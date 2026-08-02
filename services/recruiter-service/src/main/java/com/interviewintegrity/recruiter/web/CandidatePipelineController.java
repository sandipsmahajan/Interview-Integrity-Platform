package com.interviewintegrity.recruiter.web;

import com.interviewintegrity.recruiter.service.PipelineService;
import com.interviewintegrity.recruiter.service.RecruiterMapper;
import com.interviewintegrity.recruiter.web.dto.CandidatePipelineResponse;
import com.interviewintegrity.recruiter.web.dto.EnterStageRequest;
import com.interviewintegrity.recruiter.web.dto.ExitStageRequest;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Candidate pipeline movement endpoints. */
@RestController
@RequestMapping("/api/v1/candidates/{candidateId}/pipeline")
@Tag(name = "Candidate Pipeline", description = "Track candidates through hiring stages")
public final class CandidatePipelineController {

  private final PipelineService pipelineService;
  private final RecruiterMapper mapper;

  /** Creates the controller bound to the pipeline service and mapper. */
  public CandidatePipelineController(PipelineService pipelineService, RecruiterMapper mapper) {
    this.pipelineService = pipelineService;
    this.mapper = mapper;
  }

  /** Enters a candidate into a stage. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Enter a candidate into a stage")
  public Mono<CandidatePipelineResponse> enter(
      Authentication authentication,
      @PathVariable UUID candidateId,
      @Valid @RequestBody EnterStageRequest request) {
    return pipelineService
        .enterStage(
            SecurityPrincipals.organizationId(authentication),
            candidateId,
            SecurityPrincipals.userId(authentication),
            request.stageId(),
            request.position())
        .map(mapper::toResponse);
  }

  /** Lists the pipeline history of a candidate. */
  @GetMapping
  @Operation(summary = "List a candidate's pipeline history")
  public Flux<CandidatePipelineResponse> history(
      Authentication authentication, @PathVariable UUID candidateId) {
    return pipelineService
        .listByCandidate(SecurityPrincipals.organizationId(authentication), candidateId)
        .map(mapper::toResponse);
  }

  /** Moves a candidate out of the current stage. */
  @PostMapping("/exit")
  @Operation(summary = "Exit a candidate from a stage")
  public Mono<CandidatePipelineResponse> exit(
      Authentication authentication,
      @PathVariable UUID candidateId,
      @Valid @RequestBody ExitStageRequest request) {
    return pipelineService
        .exitStage(
            SecurityPrincipals.organizationId(authentication), candidateId, request.stageId())
        .map(mapper::toResponse);
  }
}
