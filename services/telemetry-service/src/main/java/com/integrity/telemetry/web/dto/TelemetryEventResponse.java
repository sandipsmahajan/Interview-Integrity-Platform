package com.integrity.telemetry.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public view of a stored raw telemetry event.
 *
 * @param id event identifier
 * @param sessionId owning session
 * @param interviewId monitored interview
 * @param eventType event type code
 * @param seq per-session sequence number
 * @param occurredAt instant the event happened
 * @param clientOccurredAt client clock instant
 * @param payload JSON event payload
 */
public record TelemetryEventResponse(
    UUID id,
    UUID sessionId,
    UUID interviewId,
    String eventType,
    long seq,
    Instant occurredAt,
    Instant clientOccurredAt,
    String payload) {}
