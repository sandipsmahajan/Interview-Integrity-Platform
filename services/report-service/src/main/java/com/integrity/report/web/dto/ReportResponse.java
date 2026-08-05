package com.integrity.report.web.dto;

import com.integrity.report.domain.ReportFormat;
import com.integrity.report.domain.ReportStatus;
import com.integrity.report.domain.ReportType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Public view of a report.
 *
 * @param id report identifier
 * @param organizationId owning tenant
 * @param type subject area
 * @param title report title
 * @param status lifecycle state
 * @param format output format
 * @param score integrity score
 * @param filters JSON filter predicates
 * @param requestedBy requesting user
 * @param requestedAt request instant
 * @param generatedAt generation instant
 * @param expiresAt expiry instant
 * @param storageObjectId attached storage object
 * @param createdAt creation instant
 * @param updatedAt last update instant
 */
public record ReportResponse(
    UUID id,
    UUID organizationId,
    ReportType type,
    String title,
    ReportStatus status,
    ReportFormat format,
    BigDecimal score,
    String filters,
    UUID requestedBy,
    Instant requestedAt,
    Instant generatedAt,
    Instant expiresAt,
    UUID storageObjectId,
    Instant createdAt,
    Instant updatedAt) {}
