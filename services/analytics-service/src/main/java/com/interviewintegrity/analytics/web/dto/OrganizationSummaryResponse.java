package com.interviewintegrity.analytics.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Public view of a daily organization summary.
 *
 * @param summaryDate summary date
 * @param organizationId owning tenant
 * @param interviewsScheduled scheduled interviews
 * @param interviewsCompleted completed interviews
 * @param interviewsCancelled cancelled interviews
 * @param candidatesActive active candidates
 * @param recruitersActive active recruiters
 * @param violations integrity violations
 * @param avgIntegrityScore average integrity score
 * @param createdAt creation instant
 * @param updatedAt last update instant
 */
public record OrganizationSummaryResponse(
    LocalDate summaryDate,
    UUID organizationId,
    long interviewsScheduled,
    long interviewsCompleted,
    long interviewsCancelled,
    long candidatesActive,
    long recruitersActive,
    long violations,
    BigDecimal avgIntegrityScore,
    Instant createdAt,
    Instant updatedAt) {}
