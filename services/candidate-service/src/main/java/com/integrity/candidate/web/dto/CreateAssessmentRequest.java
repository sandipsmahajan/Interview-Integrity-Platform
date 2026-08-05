package com.integrity.candidate.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Request to assign an assessment to a candidate.
 *
 * @param assessmentType type of the assessment
 * @param expiresAt optional expiry of the assessment link
 */
public record CreateAssessmentRequest(
    @NotBlank @Size(max = 80) String assessmentType, Instant expiresAt) {}
