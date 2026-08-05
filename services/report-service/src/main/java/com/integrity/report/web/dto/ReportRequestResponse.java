package com.integrity.report.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public view of a report request.
 *
 * @param id request identifier
 * @param organizationId owning tenant
 * @param reportId target report
 * @param aggregationLevel aggregation granularity
 * @param timeRange JSON time window
 * @param parameters JSON generation parameters
 * @param requestedBy requesting user
 * @param requestedAt request instant
 * @param completedAt completion instant
 * @param errorMessage failure detail
 */
public record ReportRequestResponse(
    UUID id,
    UUID organizationId,
    UUID reportId,
    String aggregationLevel,
    String timeRange,
    String parameters,
    UUID requestedBy,
    Instant requestedAt,
    Instant completedAt,
    String errorMessage) {}
