package com.interviewintegrity.analytics.web.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request to record a daily recruiter summary.
 *
 * @param date summary date
 * @param recruiterId recruiter identifier
 * @param interviewsHeld interviews held
 * @param interviewsCompleted completed interviews
 * @param candidatesContacted contacted candidates
 * @param avgFeedbackRating average feedback rating
 * @param violations integrity violations
 */
public record RecordRecruiterSummaryRequest(
    @NotNull LocalDate date,
    @NotNull UUID recruiterId,
    long interviewsHeld,
    long interviewsCompleted,
    long candidatesContacted,
    BigDecimal avgFeedbackRating,
    long violations) {}
