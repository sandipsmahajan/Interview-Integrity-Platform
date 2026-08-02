package com.interviewintegrity.interview.web;

import com.interviewintegrity.interview.domain.InterviewStatus;
import com.interviewintegrity.interview.service.InterviewMapper;
import com.interviewintegrity.interview.service.InterviewService;
import com.interviewintegrity.interview.web.dto.CreateInterviewRequest;
import com.interviewintegrity.interview.web.dto.InterviewResponse;
import com.interviewintegrity.interview.web.dto.ScheduleInterviewRequest;
import com.interviewintegrity.interview.web.dto.UpdateInterviewRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Interview record endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/interviews")
@Tag(name = "Interviews", description = "Manage interview records")
public final class InterviewController {

  private final InterviewService interviewService;
  private final InterviewMapper mapper;

  /** Creates the controller bound to the interview service and mapper. */
  public InterviewController(InterviewService interviewService, InterviewMapper mapper) {
    this.interviewService = interviewService;
    this.mapper = mapper;
  }

  /** Creates an interview. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create an interview")
  public Mono<InterviewResponse> create(
      Authentication authentication, @Valid @RequestBody CreateInterviewRequest request) {
    return interviewService
        .create(
            SecurityPrincipals.organizationId(authentication),
            request.candidateId(),
            request.recruiterId(),
            request.roundNumber(),
            request.title().trim(),
            request.mode(),
            request.meetingUrl(),
            request.startsAt(),
            request.endsAt(),
            request.timezone(),
            request.metadata(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Lists the interviews of the organization, optionally filtered. */
  @GetMapping
  @Operation(summary = "List interviews")
  public Flux<InterviewResponse> list(
      Authentication authentication,
      @RequestParam(required = false) InterviewStatus status,
      @RequestParam(required = false) UUID candidateId,
      @RequestParam(required = false) UUID recruiterId) {
    UUID organizationId = SecurityPrincipals.organizationId(authentication);
    if (candidateId != null) {
      return interviewService.listByCandidate(organizationId, candidateId).map(mapper::toResponse);
    }
    if (recruiterId != null) {
      return interviewService.listByRecruiter(organizationId, recruiterId).map(mapper::toResponse);
    }
    return interviewService.list(organizationId, status).map(mapper::toResponse);
  }

  /** Returns a single interview. */
  @GetMapping("/{interviewId}")
  @Operation(summary = "Get an interview")
  public Mono<InterviewResponse> get(
      Authentication authentication, @PathVariable UUID interviewId) {
    return interviewService
        .get(interviewId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Updates the mutable details of an interview. */
  @PatchMapping("/{interviewId}")
  @Operation(summary = "Update an interview")
  public Mono<InterviewResponse> update(
      Authentication authentication,
      @PathVariable UUID interviewId,
      @Valid @RequestBody UpdateInterviewRequest request) {
    return interviewService
        .update(
            interviewId,
            SecurityPrincipals.organizationId(authentication),
            request.title().trim(),
            request.meetingUrl(),
            request.metadata(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Re-schedules an interview. */
  @PostMapping("/{interviewId}/schedule")
  @Operation(summary = "Schedule an interview")
  public Mono<InterviewResponse> schedule(
      Authentication authentication,
      @PathVariable UUID interviewId,
      @Valid @RequestBody ScheduleInterviewRequest request) {
    return interviewService
        .schedule(
            interviewId,
            SecurityPrincipals.organizationId(authentication),
            request.startsAt(),
            request.endsAt(),
            request.timezone(),
            request.meetingUrl(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Cancels an interview. */
  @PostMapping("/{interviewId}/cancel")
  @Operation(summary = "Cancel an interview")
  public Mono<InterviewResponse> cancel(
      Authentication authentication, @PathVariable UUID interviewId) {
    return interviewService
        .cancel(
            interviewId,
            SecurityPrincipals.organizationId(authentication),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Marks an interview as no-show. */
  @PostMapping("/{interviewId}/no-show")
  @Operation(summary = "Mark an interview as no-show")
  public Mono<InterviewResponse> markNoShow(
      Authentication authentication, @PathVariable UUID interviewId) {
    return interviewService
        .markNoShow(
            interviewId,
            SecurityPrincipals.organizationId(authentication),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Soft deletes an interview. */
  @DeleteMapping("/{interviewId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete an interview")
  public Mono<Void> delete(Authentication authentication, @PathVariable UUID interviewId) {
    return interviewService.delete(
        interviewId,
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication));
  }
}
