package com.interviewintegrity.report.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to record the parameters of a report generation.
 *
 * @param aggregationLevel aggregation granularity
 * @param timeRange JSON time window
 * @param parameters JSON generation parameters
 */
public record CreateReportRequestRequest(
    @NotBlank @Size(max = 100) String aggregationLevel, String timeRange, String parameters) {}
