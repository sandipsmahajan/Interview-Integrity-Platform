package com.integrity.candidate.web.dto;

import com.integrity.candidate.domain.CandidateStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request to change the status of a candidate.
 *
 * @param status target lifecycle status
 */
public record ChangeCandidateStatusRequest(@NotNull CandidateStatus status) {}
