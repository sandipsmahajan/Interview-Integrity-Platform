package com.integrity.report.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to update a report section.
 *
 * @param title optional section heading
 * @param content JSON payload of the section
 * @param orderIndex display ordering
 */
public record UpdateReportSectionRequest(
    @Size(max = 255) String title, @NotBlank String content, @Min(0) int orderIndex) {}
