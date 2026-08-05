package com.integrity.telemetry.web.dto;

import java.util.UUID;

/**
 * Public view of a telemetry event type catalog entry.
 *
 * @param id identifier
 * @param code stable event type code
 * @param name display name
 * @param description description
 * @param retentionDays retention window in days
 */
public record TelemetryEventTypeResponse(
    UUID id, String code, String name, String description, int retentionDays) {}
