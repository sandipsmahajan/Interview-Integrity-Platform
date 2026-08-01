package com.interviewintegrity.telemetry.web.dto;

import com.interviewintegrity.telemetry.service.TelemetryEventData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request to ingest a batch of telemetry events for a session.
 *
 * @param events the events to store
 */
public record IngestEventsRequest(@NotNull @Valid List<TelemetryEventData> events) {}
