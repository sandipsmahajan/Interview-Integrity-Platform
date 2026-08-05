package com.integrity.telemetry.service;

import java.time.Instant;
import java.util.UUID;

/**
 * One telemetry event as carried by ingest requests and the {@code telemetry.received.v1} bus.
 *
 * @param eventId stable client-generated event identifier (idempotency key)
 * @param eventType event type code from the telemetry event catalog
 * @param seq per-session sequence number
 * @param occurredAt instant the event happened
 * @param clientOccurredAt client clock instant when the event happened
 * @param payload free form JSON event payload
 */
public record TelemetryEventData(
    UUID eventId,
    String eventType,
    long seq,
    Instant occurredAt,
    Instant clientOccurredAt,
    String payload) {}
