package com.interviewintegrity.report.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to add a section to a report.
 *
 * @param sectionType section kind
 * @param title optional section heading
 * @param content JSON payload of the section
 * @param orderIndex display ordering
 */
public record CreateReportSectionRequest(
    @NotBlank @Size(max = 100) String sectionType,
    @Size(max = 255) String title,
    @NotBlank String content,
    @Min(0) int orderIndex) {}
