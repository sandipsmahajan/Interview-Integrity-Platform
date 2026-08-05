package com.integrity.interview.web.dto;

import com.integrity.interview.domain.FeedbackStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of interview feedback.
 *
 * @param id feedback identifier
 * @param organizationId owning tenant
 * @param interviewId interview identifier
 * @param interviewerId interviewer that wrote the feedback
 * @param rating rating from 1 to 5, when provided
 * @param strengths observed strengths
 * @param concerns observed concerns
 * @param recommendation final recommendation
 * @param status state of the feedback
 * @param submittedAt instant the feedback was submitted, when any
 * @param createdAt instant the feedback was created
 * @param updatedAt instant the feedback was last modified
 */
public record InterviewFeedbackResponse(
    UUID id,
    UUID organizationId,
    UUID interviewId,
    UUID interviewerId,
    Integer rating,
    String strengths,
    String concerns,
    String recommendation,
    FeedbackStatus status,
    Instant submittedAt,
    Instant createdAt,
    Instant updatedAt) {}
