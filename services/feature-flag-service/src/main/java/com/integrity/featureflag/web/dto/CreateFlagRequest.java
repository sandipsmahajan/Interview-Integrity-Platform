package com.integrity.featureflag.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Request to create a feature flag.
 *
 * @param environment target environment
 * @param enabled whether the flag is on
 * @param rolloutPercent gradual rollout percentage
 * @param defaultVariant default variant when no rule matches
 * @param variants JSON variant definitions
 * @param rules JSON targeting rules
 */
public record CreateFlagRequest(
    @Size(max = 50) String environment,
    boolean enabled,
    @Min(0) @Max(100) int rolloutPercent,
    @Size(max = 200) String defaultVariant,
    @Size(max = 100000) String variants,
    @Size(max = 100000) String rules) {}
