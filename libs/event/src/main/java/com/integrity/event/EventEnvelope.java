package com.integrity.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Standard envelope wrapping every event published on the platform event bus.
 *
 * @param eventId unique event identifier
 * @param type event type name, e.g. {@code interview.started.v1}
 * @param service name of the publishing service
 * @param occurredAt instant at which the event occurred
 * @param payload JSON encoded event payload
 */
public record EventEnvelope(
    UUID eventId, String type, String service, Instant occurredAt, String payload) {}
