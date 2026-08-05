package com.integrity.interview.web;

import com.integrity.interview.service.InterviewMapper;
import com.integrity.interview.service.InterviewerService;
import com.integrity.interview.web.dto.CreateInterviewerRequest;
import com.integrity.interview.web.dto.InterviewerResponse;
import com.integrity.interview.web.dto.UpdateInterviewerRequest;
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

/** Interviewer profile endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/interviewers")
@Tag(name = "Interviewers", description = "Manage interviewer profiles")
public final class InterviewerController {

  private final InterviewerService interviewerService;
  private final InterviewMapper mapper;

  /** Creates the controller bound to the interviewer service and mapper. */
  public InterviewerController(InterviewerService interviewerService, InterviewMapper mapper) {
    this.interviewerService = interviewerService;
    this.mapper = mapper;
  }

  /** Creates an interviewer profile. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create an interviewer")
  public Mono<InterviewerResponse> create(
      Authentication authentication, @Valid @RequestBody CreateInterviewerRequest request) {
    return interviewerService
        .create(
            SecurityPrincipals.organizationId(authentication),
            request.userId(),
            request.fullName().trim(),
            request.email().trim(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Lists the interviewers of the organization. */
  @GetMapping
  @Operation(summary = "List interviewers")
  public Flux<InterviewerResponse> list(Authentication authentication) {
    return interviewerService
        .list(SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Returns a single interviewer. */
  @GetMapping("/{interviewerId}")
  @Operation(summary = "Get an interviewer")
  public Mono<InterviewerResponse> get(
      Authentication authentication, @PathVariable UUID interviewerId) {
    return interviewerService
        .get(interviewerId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Updates an interviewer profile. */
  @PatchMapping("/{interviewerId}")
  @Operation(summary = "Update an interviewer")
  public Mono<InterviewerResponse> update(
      Authentication authentication,
      @PathVariable UUID interviewerId,
      @Valid @RequestBody UpdateInterviewerRequest request) {
    return interviewerService
        .update(
            interviewerId,
            SecurityPrincipals.organizationId(authentication),
            request.fullName().trim(),
            request.email().trim(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Soft deletes an interviewer profile. */
  @DeleteMapping("/{interviewerId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete an interviewer")
  public Mono<Void> delete(Authentication authentication, @PathVariable UUID interviewerId) {
    return interviewerService.delete(
        interviewerId,
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication));
  }
}
