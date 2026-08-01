package com.interviewintegrity.report.web.dto;

import com.interviewintegrity.report.domain.ReportFormat;
import com.interviewintegrity.report.domain.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to create a report.
 *
 * @param type subject area of the report
 * @param title report title
 * @param format output format
 * @param filters JSON filter predicates
 */
public record CreateReportRequest(
    @NotNull ReportType type,
    @NotBlank @Size(max = 255) String title,
    @NotNull ReportFormat format,
    String filters) {}
