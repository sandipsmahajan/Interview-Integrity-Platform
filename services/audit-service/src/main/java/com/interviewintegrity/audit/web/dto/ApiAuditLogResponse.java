package com.interviewintegrity.audit.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of an HTTP access log entry.
 *
 * @param id entry identifier
 * @param organizationId owning tenant
 * @param method HTTP method
 * @param path requested path
 * @param statusCode HTTP response status
 * @param durationMs response duration in milliseconds
 * @param actorId acting user
 * @param requestId correlated request identifier
 * @param clientIp client IP address
 * @param occurredAt instant the request completed
 */
public record ApiAuditLogResponse(
    Long id,
    UUID organizationId,
    String method,
    String path,
    int statusCode,
    int durationMs,
    UUID actorId,
    String requestId,
    String clientIp,
    Instant occurredAt) {}
