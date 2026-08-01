package com.interviewintegrity.interview.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request to create a feedback record.
 *
 * @param interviewerId interviewer writing the feedback
 */
public record CreateFeedbackRequest(@NotNull UUID interviewerId) {}
