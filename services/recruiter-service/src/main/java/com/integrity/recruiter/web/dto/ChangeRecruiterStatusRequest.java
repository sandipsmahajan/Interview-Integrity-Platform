package com.integrity.recruiter.web.dto;

import com.integrity.recruiter.domain.RecruiterStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request to change the status of a recruiter.
 *
 * @param status target working status
 */
public record ChangeRecruiterStatusRequest(@NotNull RecruiterStatus status) {}
