package com.interviewintegrity.telemetry.web.dto;

import com.interviewintegrity.telemetry.domain.TelemetrySessionStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request to transition a telemetry session.
 *
 * @param status target lifecycle state
 */
public record ChangeSessionStatusRequest(@NotNull TelemetrySessionStatus status) {}
