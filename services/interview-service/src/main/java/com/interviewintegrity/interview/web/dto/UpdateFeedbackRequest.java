package com.interviewintegrity.interview.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Request to update a feedback record.
 *
 * @param rating rating from 1 to 5, when provided
 * @param strengths observed strengths
 * @param concerns observed concerns
 * @param recommendation final recommendation
 */
public record UpdateFeedbackRequest(
    @Min(1) @Max(5) Integer rating,
    @Size(max = 8000) String strengths,
    @Size(max = 8000) String concerns,
    @Size(max = 4000) String recommendation) {}
