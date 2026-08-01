package com.interviewintegrity.telemetry.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request to create a telemetry monitoring session.
 *
 * @param interviewId interview being monitored
 * @param candidateId optional candidate reference
 * @param deviceId client device identity
 * @param clientVersion client version
 * @param heartbeatCadenceSeconds client heartbeat cadence
 */
public record CreateSessionRequest(
    @NotNull UUID interviewId,
    UUID candidateId,
    String deviceId,
    String clientVersion,
    @Min(1) @Max(3600) Integer heartbeatCadenceSeconds) {}
