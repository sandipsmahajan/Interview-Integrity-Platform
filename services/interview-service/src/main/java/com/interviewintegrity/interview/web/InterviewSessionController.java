package com.interviewintegrity.interview.web;

import com.interviewintegrity.interview.domain.InterviewSession;
import com.interviewintegrity.interview.service.InterviewSessionService;
import com.interviewintegrity.interview.web.dto.InterviewSessionResponse;
import com.interviewintegrity.interview.web.dto.StartSessionRequest;
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

/** Interview monitoring session endpoints. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Interview Sessions", description = "Manage interview monitoring sessions")
public final class InterviewSessionController {

  private final InterviewSessionService sessionService;

  /** Creates the controller bound to the session service. */
  public InterviewSessionController(InterviewSessionService sessionService) {
    this.sessionService = sessionService;
  }

  /** Starts a monitoring session for an interview. */
  @PostMapping("/interviews/{interviewId}/sessions")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Start an interview session")
  public Mono<InterviewSessionResponse> start(
      Authentication authentication,
      @PathVariable UUID interviewId,
      @Valid @RequestBody StartSessionRequest request) {
    return sessionService
        .start(
            SecurityPrincipals.organizationId(authentication),
            interviewId,
            request.sessionTokenHash().trim(),
            request.deviceId(),
            request.clientVersion(),
            request.heartbeatCadenceSeconds(),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  /** Lists the sessions of an interview. */
  @GetMapping("/interviews/{interviewId}/sessions")
  @Operation(summary = "List interview sessions")
  public Flux<InterviewSessionResponse> list(
      Authentication authentication, @PathVariable UUID interviewId) {
    return sessionService
        .list(SecurityPrincipals.organizationId(authentication), interviewId)
        .map(this::toResponse);
  }

  /** Pauses an active session. */
  @PostMapping("/sessions/{sessionId}/pause")
  @Operation(summary = "Pause an interview session")
  public Mono<InterviewSessionResponse> pause(
      Authentication authentication, @PathVariable UUID sessionId) {
    return sessionService
        .pause(sessionId, SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  /** Resumes a paused session. */
  @PostMapping("/sessions/{sessionId}/resume")
  @Operation(summary = "Resume an interview session")
  public Mono<InterviewSessionResponse> resume(
      Authentication authentication, @PathVariable UUID sessionId) {
    return sessionService
        .resume(sessionId, SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  /** Completes a session and its interview. */
  @PostMapping("/sessions/{sessionId}/complete")
  @Operation(summary = "Complete an interview session")
  public Mono<InterviewSessionResponse> complete(
      Authentication authentication, @PathVariable UUID sessionId) {
    return sessionService
        .complete(
            sessionId,
            SecurityPrincipals.organizationId(authentication),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  /** Marks a session as ended abnormally. */
  @PostMapping("/sessions/{sessionId}/abnormal")
  @Operation(summary = "Mark an interview session abnormal")
  public Mono<InterviewSessionResponse> markAbnormal(
      Authentication authentication, @PathVariable UUID sessionId) {
    return sessionService
        .markAbnormal(sessionId, SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  private InterviewSessionResponse toResponse(InterviewSession session) {
    return new InterviewSessionResponse(
        session.getId(),
        session.getOrganizationId(),
        session.getInterviewId(),
        session.getStatus(),
        session.getDeviceId(),
        session.getClientVersion(),
        session.getStartedAt(),
        session.getEndedAt(),
        session.getHeartbeatCadenceSeconds(),
        session.getCreatedAt());
  }
}
