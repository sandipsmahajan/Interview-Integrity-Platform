package com.integrity.featureflag.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a feature flag.
 *
 * @param id flag identifier
 * @param organizationId owning tenant
 * @param featureId owning feature
 * @param environment target environment
 * @param enabled whether the flag is on
 * @param rolloutPercent gradual rollout percentage
 * @param defaultVariant default variant when no rule matches
 * @param variants JSON variant definitions
 * @param rules JSON targeting rules
 * @param createdAt instant the flag was created
 * @param updatedAt instant the flag was last modified
 */
public record FeatureFlagResponse(
    UUID id,
    UUID organizationId,
    UUID featureId,
    String environment,
    boolean enabled,
    int rolloutPercent,
    String defaultVariant,
    String variants,
    String rules,
    Instant createdAt,
    Instant updatedAt) {}
