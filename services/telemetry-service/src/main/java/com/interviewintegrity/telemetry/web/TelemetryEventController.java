package com.interviewintegrity.telemetry.web;

import com.interviewintegrity.security.SecurityPrincipals;
import com.interviewintegrity.telemetry.service.TelemetryEventService;
import com.interviewintegrity.telemetry.service.TelemetryMapper;
import com.interviewintegrity.telemetry.web.dto.IngestEventsRequest;
import com.interviewintegrity.telemetry.web.dto.TelemetryEventResponse;
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

/** Telemetry event ingestion and query endpoints. */
@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/events")
@Tag(name = "Events", description = "Ingest and query raw telemetry events")
public final class TelemetryEventController {

  private final TelemetryEventService eventService;
  private final TelemetryMapper mapper;

  /** Creates the controller bound to the event service and mapper. */
  public TelemetryEventController(TelemetryEventService eventService, TelemetryMapper mapper) {
    this.eventService = eventService;
    this.mapper = mapper;
  }

  /** Ingests a batch of events for a session. */
  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Operation(summary = "Ingest telemetry events")
  public Flux<TelemetryEventResponse> ingest(
      Authentication authentication,
      @PathVariable UUID sessionId,
      @Valid @RequestBody IngestEventsRequest request) {
    return eventService
        .ingest(SecurityPrincipals.organizationId(authentication), sessionId, request.events())
        .map(mapper::toResponse);
  }

  /** Lists the events of a session, optionally filtered by event type. */
  @GetMapping
  @Operation(summary = "List telemetry events")
  public Flux<TelemetryEventResponse> list(
      Authentication authentication,
      @PathVariable UUID sessionId,
      @RequestParam(required = false) String eventType) {
    return eventService
        .list(SecurityPrincipals.organizationId(authentication), sessionId, eventType)
        .map(mapper::toResponse);
  }

  /** Counts the events of a session. */
  @GetMapping("/count")
  @Operation(summary = "Count telemetry events")
  public Mono<Long> count(Authentication authentication, @PathVariable UUID sessionId) {
    return eventService.count(SecurityPrincipals.organizationId(authentication), sessionId);
  }
}
