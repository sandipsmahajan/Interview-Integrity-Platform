package com.interviewintegrity.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Cross-service representation of a recorded telemetry event.
 *
 * @param id event identifier
 * @param sessionId owning session identifier
 * @param interviewId interview the session belongs to
 * @param type telemetry event type
 * @param occurredAt instant the event occurred
 * @param payload free form event payload
 */
public record TelemetryEventDto(
    UUID id, UUID sessionId, UUID interviewId, String type, Instant occurredAt, String payload) {}
