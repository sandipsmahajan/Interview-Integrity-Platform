package com.interviewintegrity.candidate.web;

import com.interviewintegrity.candidate.service.AssessmentService;
import com.interviewintegrity.candidate.service.CandidateMapper;
import com.interviewintegrity.candidate.web.dto.AssessmentResponse;
import com.interviewintegrity.candidate.web.dto.CompleteAssessmentRequest;
import com.interviewintegrity.candidate.web.dto.CreateAssessmentRequest;
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

/** Assessment endpoints scoped to a candidate. */
@RestController
@RequestMapping("/api/v1/candidates/{candidateId}/assessments")
@Tag(name = "Assessments", description = "Manage assessments assigned to candidates")
public final class AssessmentController {

  private final AssessmentService assessmentService;
  private final CandidateMapper mapper;

  /** Creates the controller bound to the assessment service and mapper. */
  public AssessmentController(AssessmentService assessmentService, CandidateMapper mapper) {
    this.assessmentService = assessmentService;
    this.mapper = mapper;
  }

  /** Assigns an assessment to a candidate. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Assign an assessment to a candidate")
  public Mono<AssessmentResponse> create(
      Authentication authentication,
      @PathVariable UUID candidateId,
      @Valid @RequestBody CreateAssessmentRequest request) {
    return assessmentService
        .create(
            SecurityPrincipals.organizationId(authentication),
            candidateId,
            request.assessmentType(),
            request.expiresAt(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Lists the assessments of a candidate. */
  @GetMapping
  @Operation(summary = "List candidate assessments")
  public Flux<AssessmentResponse> list(
      Authentication authentication, @PathVariable UUID candidateId) {
    return assessmentService
        .list(SecurityPrincipals.organizationId(authentication), candidateId)
        .map(mapper::toResponse);
  }

  /** Starts an assigned assessment. */
  @PostMapping("/{assessmentId}/start")
  @Operation(summary = "Start an assessment")
  public Mono<AssessmentResponse> start(
      Authentication authentication, @PathVariable UUID assessmentId) {
    return assessmentService
        .start(assessmentId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Completes an assessment with an optional score. */
  @PostMapping("/{assessmentId}/complete")
  @Operation(summary = "Complete an assessment")
  public Mono<AssessmentResponse> complete(
      Authentication authentication,
      @PathVariable UUID assessmentId,
      @Valid @RequestBody CompleteAssessmentRequest request) {
    return assessmentService
        .complete(assessmentId, SecurityPrincipals.organizationId(authentication), request.score())
        .map(mapper::toResponse);
  }

  /** Expires an assessment. */
  @PostMapping("/{assessmentId}/expire")
  @Operation(summary = "Expire an assessment")
  public Mono<AssessmentResponse> expire(
      Authentication authentication, @PathVariable UUID assessmentId) {
    return assessmentService
        .expire(assessmentId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }
}
