package com.integrity.report.web.dto;

import com.integrity.report.domain.ReportFormat;
import com.integrity.report.domain.ReportType;
import java.time.Instant;
import java.util.UUID;

/**
 * Public view of a report schedule.
 *
 * @param id schedule identifier
 * @param organizationId owning tenant
 * @param type subject area
 * @param cronExpression cron trigger expression
 * @param format output format
 * @param recipients JSON recipient list
 * @param parameters JSON schedule parameters
 * @param enabled whether the schedule is active
 * @param nextRunAt next scheduled run
 * @param lastRunAt last executed run
 * @param createdAt creation instant
 * @param updatedAt last update instant
 */
public record ReportScheduleResponse(
    UUID id,
    UUID organizationId,
    ReportType type,
    String cronExpression,
    ReportFormat format,
    String recipients,
    String parameters,
    boolean enabled,
    Instant nextRunAt,
    Instant lastRunAt,
    Instant createdAt,
    Instant updatedAt) {}
