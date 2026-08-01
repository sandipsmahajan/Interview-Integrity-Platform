package com.interviewintegrity.analytics.web.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request to record a daily interview summary.
 *
 * @param date summary date
 * @param interviewId interview identifier
 * @param durationMinutes interview duration
 * @param integrityScore integrity score
 * @param violations integrity violations
 * @param status interview status
 */
public record RecordInterviewSummaryRequest(
    @NotNull LocalDate date,
    @NotNull UUID interviewId,
    Integer durationMinutes,
    BigDecimal integrityScore,
    long violations,
    String status) {}
