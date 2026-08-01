package com.interviewintegrity.report.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to mark a report request as failed.
 *
 * @param errorMessage failure detail
 */
public record FailReportRequest(@NotBlank @Size(max = 2000) String errorMessage) {}
