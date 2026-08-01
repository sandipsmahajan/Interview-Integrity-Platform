package com.interviewintegrity.policy.web.dto;

import com.interviewintegrity.policy.domain.PolicyStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request to transition a policy lifecycle state.
 *
 * @param status target state
 */
public record ChangePolicyStatusRequest(@NotNull PolicyStatus status) {}
