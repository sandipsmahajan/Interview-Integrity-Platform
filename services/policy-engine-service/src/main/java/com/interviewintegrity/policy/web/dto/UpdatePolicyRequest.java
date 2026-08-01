package com.interviewintegrity.policy.web.dto;

import com.interviewintegrity.policy.domain.ViolationSeverity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to update the editable attributes of a policy.
 *
 * @param name display name
 * @param description description
 * @param defaultSeverity default severity for unmatched rules
 * @param priority evaluation priority (lower runs first)
 * @param enabled whether the policy participates in evaluation
 */
public record UpdatePolicyRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 1000) String description,
    ViolationSeverity defaultSeverity,
    @Min(0) @Max(1000) Integer priority,
    Boolean enabled) {}
