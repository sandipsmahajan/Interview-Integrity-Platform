package com.integrity.telemetry.web;

import com.integrity.telemetry.service.TelemetryEventTypeService;
import com.integrity.telemetry.service.TelemetryMapper;
import com.integrity.telemetry.web.dto.TelemetryEventTypeResponse;
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
  private final TelemetryMapper mapper;

  /** Creates the controller bound to the event type service and mapper. */
  public TelemetryEventTypeController(
      TelemetryEventTypeService eventTypeService, TelemetryMapper mapper) {
    this.eventTypeService = eventTypeService;
    this.mapper = mapper;
  }

  /** Lists the event type catalog. */
  @GetMapping
  @Operation(summary = "List telemetry event types")
  public Flux<TelemetryEventTypeResponse> list() {
    return eventTypeService.list().map(mapper::toResponse);
  }
}
