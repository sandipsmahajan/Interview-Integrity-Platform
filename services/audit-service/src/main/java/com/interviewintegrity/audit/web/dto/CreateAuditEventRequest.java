package com.interviewintegrity.audit.web.dto;

import com.interviewintegrity.audit.domain.AuditOutcome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/**
 * Request to record a compliance audit event.
 *
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
public record CreateAuditEventRequest(
    UUID actorId,
    @Size(max = 50) String actorType,
    @NotBlank @Size(max = 200) String action,
    @NotBlank @Size(max = 200) String resourceType,
    UUID resourceId,
    AuditOutcome outcome,
    Instant occurredAt,
    @Size(max = 200) String requestId,
    String ipAddress,
    @Size(max = 1000) String userAgent,
    @Size(max = 100000) String metadata) {}
