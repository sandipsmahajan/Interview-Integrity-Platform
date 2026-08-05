package com.integrity.analytics.web.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request to record a daily organization summary.
 *
 * @param date summary date
 * @param interviewsScheduled scheduled interviews
 * @param interviewsCompleted completed interviews
 * @param interviewsCancelled cancelled interviews
 * @param candidatesActive active candidates
 * @param recruitersActive active recruiters
 * @param violations integrity violations
 * @param avgIntegrityScore average integrity score
 */
public record RecordOrganizationSummaryRequest(
    @NotNull LocalDate date,
    long interviewsScheduled,
    long interviewsCompleted,
    long interviewsCancelled,
    long candidatesActive,
    long recruitersActive,
    long violations,
    BigDecimal avgIntegrityScore) {}
