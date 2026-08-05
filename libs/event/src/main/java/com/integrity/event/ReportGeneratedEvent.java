package com.integrity.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a report has been generated and is available for download.
 *
 * @param reportId generated report identifier
 * @param organizationId owning tenant identifier
 * @param type report type name
 * @param title report title
 * @param format report format name
 * @param generatedAt instant the report finished generating
 */
public record ReportGeneratedEvent(
    UUID reportId,
    UUID organizationId,
    String type,
    String title,
    String format,
    Instant generatedAt) {}
