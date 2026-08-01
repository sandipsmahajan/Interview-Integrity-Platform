package com.interviewintegrity.telemetry.web;

import com.interviewintegrity.telemetry.domain.TelemetryEventType;
import com.interviewintegrity.telemetry.service.TelemetryEventTypeService;
import com.interviewintegrity.telemetry.web.dto.TelemetryEventTypeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/** Telemetry event type catalog endpoints. */
@RestController
@RequestMapping("/api/v1/event-types")
@Tag(name = "Event Types", description = "Telemetry event type catalog")
public final class TelemetryEventTypeController {

  private final TelemetryEventTypeService eventTypeService;

  /** Creates the controller bound to the event type service. */
  public TelemetryEventTypeController(TelemetryEventTypeService eventTypeService) {
    this.eventTypeService = eventTypeService;
  }

  /** Lists the event type catalog. */
  @GetMapping
  @Operation(summary = "List telemetry event types")
  public Flux<TelemetryEventTypeResponse> list() {
    return eventTypeService.list().map(this::toResponse);
  }

  private TelemetryEventTypeResponse toResponse(TelemetryEventType type) {
    return new TelemetryEventTypeResponse(
        type.getId(),
        type.getCode(),
        type.getName(),
        type.getDescription(),
        type.getRetentionDays());
  }
}
