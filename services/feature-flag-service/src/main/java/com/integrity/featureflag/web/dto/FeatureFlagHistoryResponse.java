package com.integrity.featureflag.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a feature flag history snapshot.
 *
 * @param historyId history identifier
 * @param historyAction database operation that produced the snapshot
 * @param changedBy user that applied the change
 * @param changedAt instant the change was applied
 * @param featureFlagId snapshot flag identifier
 * @param environment target environment
 * @param enabled whether the flag was on
 * @param rolloutPercent gradual rollout percentage
 * @param defaultVariant default variant
 * @param variants JSON variant definitions
 * @param rules JSON targeting rules
 * @param version flag version after the change
 */
public record FeatureFlagHistoryResponse(
    Long historyId,
    String historyAction,
    UUID changedBy,
    Instant changedAt,
    UUID featureFlagId,
    String environment,
    boolean enabled,
    int rolloutPercent,
    String defaultVariant,
    String variants,
    String rules,
    long version) {}
