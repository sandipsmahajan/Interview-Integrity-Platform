package com.interviewintegrity.interview.web;

import com.interviewintegrity.interview.domain.InterviewFeedback;
import com.interviewintegrity.interview.service.InterviewFeedbackService;
import com.interviewintegrity.interview.web.dto.CreateFeedbackRequest;
import com.interviewintegrity.interview.web.dto.InterviewFeedbackResponse;
import com.interviewintegrity.interview.web.dto.UpdateFeedbackRequest;
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

/** Interview feedback endpoints. */
@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/feedback")
@Tag(name = "Interview Feedback", description = "Manage interview feedback")
public final class InterviewFeedbackController {

  private final InterviewFeedbackService feedbackService;

  /** Creates the controller bound to the feedback service. */
  public InterviewFeedbackController(InterviewFeedbackService feedbackService) {
    this.feedbackService = feedbackService;
  }

  /** Creates a draft feedback record. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create interview feedback")
  public Mono<InterviewFeedbackResponse> create(
      Authentication authentication,
      @PathVariable UUID interviewId,
      @Valid @RequestBody CreateFeedbackRequest request) {
    return feedbackService
        .create(
            SecurityPrincipals.organizationId(authentication),
            interviewId,
            request.interviewerId(),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  /** Lists the feedback of an interview. */
  @GetMapping
  @Operation(summary = "List interview feedback")
  public Flux<InterviewFeedbackResponse> list(
      Authentication authentication, @PathVariable UUID interviewId) {
    return feedbackService
        .list(SecurityPrincipals.organizationId(authentication), interviewId)
        .map(this::toResponse);
  }

  /** Updates a draft feedback record. */
  @PatchMapping("/{feedbackId}")
  @Operation(summary = "Update interview feedback")
  public Mono<InterviewFeedbackResponse> update(
      Authentication authentication,
      @PathVariable UUID interviewId,
      @PathVariable UUID feedbackId,
      @Valid @RequestBody UpdateFeedbackRequest request) {
    return feedbackService
        .update(
            feedbackId,
            SecurityPrincipals.organizationId(authentication),
            request.rating(),
            request.strengths(),
            request.concerns(),
            request.recommendation(),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  /** Submits a draft feedback record. */
  @PostMapping("/{feedbackId}/submit")
  @Operation(summary = "Submit interview feedback")
  public Mono<InterviewFeedbackResponse> submit(
      Authentication authentication,
      @PathVariable UUID interviewId,
      @PathVariable UUID feedbackId) {
    return feedbackService
        .submit(
            feedbackId,
            SecurityPrincipals.organizationId(authentication),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  /** Soft deletes a feedback record. */
  @DeleteMapping("/{feedbackId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete interview feedback")
  public Mono<Void> delete(
      Authentication authentication,
      @PathVariable UUID interviewId,
      @PathVariable UUID feedbackId) {
    return feedbackService.delete(
        feedbackId,
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication));
  }

  private InterviewFeedbackResponse toResponse(InterviewFeedback feedback) {
    return new InterviewFeedbackResponse(
        feedback.getId(),
        feedback.getOrganizationId(),
        feedback.getInterviewId(),
        feedback.getInterviewerId(),
        feedback.getRating(),
        feedback.getStrengths(),
        feedback.getConcerns(),
        feedback.getRecommendation(),
        feedback.getStatus(),
        feedback.getSubmittedAt(),
        feedback.getCreatedAt(),
        feedback.getUpdatedAt());
  }
}
