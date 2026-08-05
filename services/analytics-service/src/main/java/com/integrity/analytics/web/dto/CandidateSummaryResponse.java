package com.integrity.analytics.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Public view of a daily candidate summary.
 *
 * @param summaryDate summary date
 * @param organizationId owning tenant
 * @param candidateId candidate identifier
 * @param interviewsAttended interviews attended
 * @param avgScore average score
 * @param assessmentsCompleted completed assessments
 * @param violations integrity violations
 * @param createdAt creation instant
 * @param updatedAt last update instant
 */
public record CandidateSummaryResponse(
    LocalDate summaryDate,
    UUID organizationId,
    UUID candidateId,
    long interviewsAttended,
    BigDecimal avgScore,
    long assessmentsCompleted,
    long violations,
    Instant createdAt,
    Instant updatedAt) {}
