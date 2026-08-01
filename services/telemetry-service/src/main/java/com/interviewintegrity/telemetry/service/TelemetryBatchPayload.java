package com.interviewintegrity.telemetry.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Payload of the {@code telemetry.received.v1} event.
 *
 * <p>A single payload can both refresh the session (lifecycle fields) and carry a batch of raw
 * events, so clients need to publish one event per heartbeat or event batch.
 *
 * @param sessionId client-managed session identifier
 * @param interviewId soft reference into the interview service
 * @param candidateId optional candidate reference
 * @param deviceId client device identity
 * @param clientVersion client version
 * @param heartbeatCadenceSeconds client heartbeat cadence
 * @param sessionStatus optional lifecycle transition ({@code STARTED}, {@code ACTIVE}, {@code
 *     ENDED}, {@code ABANDONED})
 * @param occurredAt event instant (used when a status transition is sent)
 * @param events optional batch of raw telemetry events
 */
public record TelemetryBatchPayload(
    UUID sessionId,
    UUID interviewId,
    UUID candidateId,
    String deviceId,
    String clientVersion,
    Integer heartbeatCadenceSeconds,
    String sessionStatus,
    Instant occurredAt,
    List<TelemetryEventData> events) {}
