package com.integrity.telemetry.service;

import com.integrity.telemetry.domain.TelemetryEvent;
import com.integrity.telemetry.domain.TelemetryEventType;
import com.integrity.telemetry.domain.TelemetrySession;
import com.integrity.telemetry.web.dto.TelemetryEventResponse;
import com.integrity.telemetry.web.dto.TelemetryEventTypeResponse;
import com.integrity.telemetry.web.dto.TelemetrySessionResponse;
import org.mapstruct.Mapper;

/**
 * Maps telemetry-service domain entities into their wire DTO records.
 *
 * <p>Field names on the records match the entity accessors, so MapStruct generates the
 * implementations at compile time.
 */
@Mapper(componentModel = "spring")
public interface TelemetryMapper {

  /** Maps a telemetry event type into its public response. */
  TelemetryEventTypeResponse toResponse(TelemetryEventType type);

  /** Maps a telemetry session into its public response. */
  TelemetrySessionResponse toResponse(TelemetrySession session);

  /** Maps a telemetry event into its public response. */
  TelemetryEventResponse toResponse(TelemetryEvent event);
}
