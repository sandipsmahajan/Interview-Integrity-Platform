package com.integrity.telemetry.service;

import com.integrity.telemetry.domain.TelemetryEventType;
import com.integrity.telemetry.repository.TelemetryEventTypeRepository;
import reactor.core.publisher.Flux;

/** Provides the global telemetry event type catalog. */
public class TelemetryEventTypeService {

  private final TelemetryEventTypeRepository eventTypeRepository;

  /** Wires the service with its repository. */
  public TelemetryEventTypeService(TelemetryEventTypeRepository eventTypeRepository) {
    this.eventTypeRepository = eventTypeRepository;
  }

  /** Lists the event type catalog ordered by code. */
  public Flux<TelemetryEventType> list() {
    return eventTypeRepository.list();
  }
}
