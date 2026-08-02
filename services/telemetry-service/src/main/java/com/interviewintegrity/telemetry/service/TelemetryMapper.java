package com.interviewintegrity.telemetry.service;

import com.interviewintegrity.telemetry.domain.TelemetryEvent;
import com.interviewintegrity.telemetry.domain.TelemetryEventType;
import com.interviewintegrity.telemetry.domain.TelemetrySession;
import com.interviewintegrity.telemetry.web.dto.TelemetryEventResponse;
import com.interviewintegrity.telemetry.web.dto.TelemetryEventTypeResponse;
import com.interviewintegrity.telemetry.web.dto.TelemetrySessionResponse;
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
