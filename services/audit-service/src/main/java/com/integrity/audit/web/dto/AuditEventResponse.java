package com.integrity.audit.web.dto;

import com.integrity.audit.domain.AuditOutcome;
import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a compliance audit event.
 *
 * @param id event identifier
 * @param organizationId owning tenant
 * @param actorId acting user
 * @param actorType type of the acting principal
 * @param action action that was performed
 * @param resourceType type of the affected resource
 * @param resourceId id of the affected resource
 * @param outcome result of the action
 * @param occurredAt instant the action occurred
 * @param requestId correlated request identifier
 * @param ipAddress client IP address
 * @param userAgent client user agent
 * @param metadata JSON metadata
 */
public record AuditEventResponse(
    UUID id,
    UUID organizationId,
    UUID actorId,
    String actorType,
    String action,
    String resourceType,
    UUID resourceId,
    AuditOutcome outcome,
    Instant occurredAt,
    String requestId,
    String ipAddress,
    String userAgent,
    String metadata) {}
