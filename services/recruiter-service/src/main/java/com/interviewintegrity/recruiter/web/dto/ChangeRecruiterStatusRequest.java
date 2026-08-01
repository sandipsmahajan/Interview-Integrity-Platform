package com.interviewintegrity.recruiter.web.dto;

import com.interviewintegrity.recruiter.domain.RecruiterStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request to change the status of a recruiter.
 *
 * @param status target working status
 */
public record ChangeRecruiterStatusRequest(@NotNull RecruiterStatus status) {}
