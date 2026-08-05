package com.integrity.report.web.dto;

import com.integrity.report.domain.ReportFormat;
import com.integrity.report.domain.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Request to create a recurring report schedule.
 *
 * @param type subject area
 * @param cronExpression cron trigger expression
 * @param format output format
 * @param recipients JSON recipient list
 * @param parameters JSON schedule parameters
 * @param nextRunAt next scheduled run
 */
public record CreateReportScheduleRequest(
    @NotNull ReportType type,
    @NotBlank @Size(max = 100) String cronExpression,
    @NotNull ReportFormat format,
    String recipients,
    String parameters,
    Instant nextRunAt) {}
