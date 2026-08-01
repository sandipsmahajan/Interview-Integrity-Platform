package com.interviewintegrity.report.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public view of a report section.
 *
 * @param id section identifier
 * @param organizationId owning tenant
 * @param reportId parent report
 * @param sectionType section kind
 * @param title section heading
 * @param content JSON payload
 * @param orderIndex display ordering
 * @param createdAt creation instant
 */
public record ReportSectionResponse(
    UUID id,
    UUID organizationId,
    UUID reportId,
    String sectionType,
    String title,
    String content,
    int orderIndex,
    Instant createdAt) {}
