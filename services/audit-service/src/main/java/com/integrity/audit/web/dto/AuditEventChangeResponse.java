package com.integrity.audit.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a field-level change within an audit event.
 *
 * @param id change identifier
 * @param auditEventId owning audit event
 * @param occurredAt instant the change was recorded
 * @param field changed field name
 * @param oldValue previous value
 * @param newValue new value
 */
public record AuditEventChangeResponse(
    Long id,
    UUID auditEventId,
    Instant occurredAt,
    String field,
    String oldValue,
    String newValue) {}
