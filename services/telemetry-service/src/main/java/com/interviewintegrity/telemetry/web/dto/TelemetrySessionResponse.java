package com.interviewintegrity.telemetry.web.dto;

import com.interviewintegrity.telemetry.domain.TelemetrySessionStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Public view of a telemetry monitoring session.
 *
 * @param id session identifier
 * @param organizationId owning tenant
 * @param interviewId monitored interview
 * @param candidateId optional candidate reference
 * @param deviceId client device identity
 * @param clientVersion client version
 * @param status lifecycle state
 * @param heartbeatCadenceSeconds client heartbeat cadence
 * @param startedAt instant the session started
 * @param endedAt instant the session ended, if any
 * @param createdAt instant the session row was created
 */
public record TelemetrySessionResponse(
    UUID id,
    UUID organizationId,
    UUID interviewId,
    UUID candidateId,
    String deviceId,
    String clientVersion,
    TelemetrySessionStatus status,
    int heartbeatCadenceSeconds,
    Instant startedAt,
    Instant endedAt,
    Instant createdAt) {}
