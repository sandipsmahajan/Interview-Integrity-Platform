package com.interviewintegrity.interview.web.dto;

import com.interviewintegrity.interview.domain.SessionStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a monitoring session.
 *
 * @param id session identifier
 * @param organizationId owning tenant
 * @param interviewId interview being monitored
 * @param status lifecycle state
 * @param deviceId device identifier of the client
 * @param clientVersion version of the client application
 * @param startedAt instant the session became active
 * @param endedAt instant the session ended, when any
 * @param heartbeatCadenceSeconds expected heartbeat interval
 * @param createdAt instant the session was created
 */
public record InterviewSessionResponse(
    UUID id,
    UUID organizationId,
    UUID interviewId,
    SessionStatus status,
    String deviceId,
    String clientVersion,
    Instant startedAt,
    Instant endedAt,
    int heartbeatCadenceSeconds,
    Instant createdAt) {}
