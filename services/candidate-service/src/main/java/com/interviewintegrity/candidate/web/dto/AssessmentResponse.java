package com.interviewintegrity.candidate.web.dto;

import com.interviewintegrity.candidate.domain.AssessmentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of an assessment.
 *
 * @param id assessment identifier
 * @param candidateId assessed candidate
 * @param assessmentType type of the assessment
 * @param status lifecycle status
 * @param score recorded score, when completed
 * @param assignedBy user that assigned the assessment
 * @param assignedAt instant the assessment was assigned
 * @param startedAt instant the assessment was started
 * @param completedAt instant the assessment was completed
 * @param expiresAt optional expiry of the assessment link
 */
public record AssessmentResponse(
    UUID id,
    UUID candidateId,
    String assessmentType,
    AssessmentStatus status,
    BigDecimal score,
    UUID assignedBy,
    Instant assignedAt,
    Instant startedAt,
    Instant completedAt,
    Instant expiresAt) {}
