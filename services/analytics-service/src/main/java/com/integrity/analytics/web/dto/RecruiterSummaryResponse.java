package com.integrity.analytics.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Public view of a daily recruiter summary.
 *
 * @param summaryDate summary date
 * @param organizationId owning tenant
 * @param recruiterId recruiter identifier
 * @param interviewsHeld interviews held
 * @param interviewsCompleted completed interviews
 * @param candidatesContacted contacted candidates
 * @param avgFeedbackRating average feedback rating
 * @param violations integrity violations
 * @param createdAt creation instant
 * @param updatedAt last update instant
 */
public record RecruiterSummaryResponse(
    LocalDate summaryDate,
    UUID organizationId,
    UUID recruiterId,
    long interviewsHeld,
    long interviewsCompleted,
    long candidatesContacted,
    BigDecimal avgFeedbackRating,
    long violations,
    Instant createdAt,
    Instant updatedAt) {}
