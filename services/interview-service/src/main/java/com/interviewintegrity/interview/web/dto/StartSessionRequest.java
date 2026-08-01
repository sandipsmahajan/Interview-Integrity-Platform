package com.interviewintegrity.interview.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to start a monitoring session.
 *
 * @param sessionTokenHash SHA-256 hash of the session token
 * @param deviceId device identifier of the client
 * @param clientVersion version of the client application
 * @param heartbeatCadenceSeconds expected heartbeat interval
 */
public record StartSessionRequest(
    @NotBlank @Size(max = 128) String sessionTokenHash,
    @Size(max = 128) String deviceId,
    @Size(max = 64) String clientVersion,
    @Min(1) @Max(3600) int heartbeatCadenceSeconds) {}
