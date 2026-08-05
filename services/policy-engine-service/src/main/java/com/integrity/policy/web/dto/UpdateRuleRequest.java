package com.integrity.policy.web.dto;

import com.integrity.policy.domain.ViolationSeverity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to update the editable attributes of a rule.
 *
 * @param ruleCode rule code within the policy
 * @param description description
 * @param condition JSONB predicate evaluated against telemetry events
 * @param severity violation severity
 * @param weight relative weight
 * @param orderIndex evaluation order
 * @param enabled whether the rule participates in evaluation
 */
public record UpdateRuleRequest(
    @NotBlank @Size(max = 100) String ruleCode,
    @Size(max = 1000) String description,
    @NotBlank @Size(max = 2000) String condition,
    ViolationSeverity severity,
    @Min(0) @Max(1000) Integer weight,
    @Min(0) @Max(10000) Integer orderIndex,
    Boolean enabled) {}
