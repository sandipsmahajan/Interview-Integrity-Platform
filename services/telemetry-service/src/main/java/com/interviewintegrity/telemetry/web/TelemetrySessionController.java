package com.interviewintegrity.telemetry.web;

import com.interviewintegrity.security.SecurityPrincipals;
import com.interviewintegrity.telemetry.domain.TelemetryEventSummary;
import com.interviewintegrity.telemetry.domain.TelemetrySession;
import com.interviewintegrity.telemetry.service.TelemetrySessionService;
import com.interviewintegrity.telemetry.web.dto.ChangeSessionStatusRequest;
import com.interviewintegrity.telemetry.web.dto.CreateSessionRequest;
import com.interviewintegrity.telemetry.web.dto.TelemetrySessionResponse;
import com.interviewintegrity.telemetry.web.dto.TelemetrySummaryResponse;
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

/** Telemetry session endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/sessions")
@Tag(name = "Sessions", description = "Manage telemetry monitoring sessions")
public final class TelemetrySessionController {

  private final TelemetrySessionService sessionService;

  /** Creates the controller bound to the session service. */
  public TelemetrySessionController(TelemetrySessionService sessionService) {
    this.sessionService = sessionService;
  }

  /** Creates a monitoring session. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a telemetry session")
  public Mono<TelemetrySessionResponse> create(
      Authentication authentication, @Valid @RequestBody CreateSessionRequest request) {
    return sessionService
        .create(
            SecurityPrincipals.organizationId(authentication),
            request.interviewId(),
            request.candidateId(),
            request.deviceId(),
            request.clientVersion(),
            request.heartbeatCadenceSeconds())
        .map(this::toResponse);
  }

  /** Lists the sessions of the organization, newest first. */
  @GetMapping
  @Operation(summary = "List telemetry sessions")
  public Flux<TelemetrySessionResponse> list(Authentication authentication) {
    return sessionService
        .list(SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  /** Returns a single session. */
  @GetMapping("/{sessionId}")
  @Operation(summary = "Get a telemetry session")
  public Mono<TelemetrySessionResponse> get(
      Authentication authentication, @PathVariable UUID sessionId) {
    return sessionService
        .get(SecurityPrincipals.organizationId(authentication), sessionId)
        .map(this::toResponse);
  }

  /** Transitions a session lifecycle state. */
  @PostMapping("/{sessionId}/status")
  @Operation(summary = "Change telemetry session status")
  public Mono<TelemetrySessionResponse> changeStatus(
      Authentication authentication,
      @PathVariable UUID sessionId,
      @Valid @RequestBody ChangeSessionStatusRequest request) {
    return sessionService
        .changeStatus(
            SecurityPrincipals.organizationId(authentication), sessionId, request.status(), null)
        .map(this::toResponse);
  }

  /** Returns the hourly rollups of a session. */
  @GetMapping("/{sessionId}/summary")
  @Operation(summary = "Get telemetry session summary")
  public Flux<TelemetrySummaryResponse> summary(
      Authentication authentication, @PathVariable UUID sessionId) {
    return sessionService
        .summaries(SecurityPrincipals.organizationId(authentication), sessionId)
        .map(this::toResponse);
  }

  private TelemetrySessionResponse toResponse(TelemetrySession session) {
    return new TelemetrySessionResponse(
        session.getId(),
        session.getOrganizationId(),
        session.getInterviewId(),
        session.getCandidateId(),
        session.getDeviceId(),
        session.getClientVersion(),
        session.getStatus(),
        session.getHeartbeatCadenceSeconds(),
        session.getStartedAt(),
        session.getEndedAt(),
        session.getCreatedAt());
  }

  private TelemetrySummaryResponse toResponse(TelemetryEventSummary summary) {
    return new TelemetrySummaryResponse(
        summary.getBucketStart(),
        summary.getBucketEnd(),
        summary.getSessionId(),
        summary.getEventType(),
        summary.getEventCount(),
        summary.getMinSeq(),
        summary.getMaxSeq(),
        summary.getLastPayload());
  }
}
