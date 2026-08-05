package com.integrity.analytics.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Public view of a daily interview summary.
 *
 * @param summaryDate summary date
 * @param organizationId owning tenant
 * @param interviewId interview identifier
 * @param durationMinutes interview duration
 * @param integrityScore integrity score
 * @param violations integrity violations
 * @param status interview status
 * @param createdAt creation instant
 * @param updatedAt last update instant
 */
public record InterviewSummaryResponse(
    LocalDate summaryDate,
    UUID organizationId,
    UUID interviewId,
    Integer durationMinutes,
    BigDecimal integrityScore,
    long violations,
    String status,
    Instant createdAt,
    Instant updatedAt) {}
