package com.integrity.analytics.web.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request to record a daily candidate summary.
 *
 * @param date summary date
 * @param candidateId candidate identifier
 * @param interviewsAttended interviews attended
 * @param avgScore average score
 * @param assessmentsCompleted completed assessments
 * @param violations integrity violations
 */
public record RecordCandidateSummaryRequest(
    @NotNull LocalDate date,
    @NotNull UUID candidateId,
    long interviewsAttended,
    BigDecimal avgScore,
    long assessmentsCompleted,
    long violations) {}
