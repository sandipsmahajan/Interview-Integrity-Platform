package com.interviewintegrity.report.web.dto;

import com.interviewintegrity.report.domain.ReportFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Request to update a recurring report schedule.
 *
 * @param cronExpression cron trigger expression
 * @param format output format
 * @param recipients JSON recipient list
 * @param parameters JSON schedule parameters
 * @param nextRunAt next scheduled run
 */
public record UpdateReportScheduleRequest(
    @NotBlank @Size(max = 100) String cronExpression,
    @NotNull ReportFormat format,
    String recipients,
    String parameters,
    Instant nextRunAt) {}
