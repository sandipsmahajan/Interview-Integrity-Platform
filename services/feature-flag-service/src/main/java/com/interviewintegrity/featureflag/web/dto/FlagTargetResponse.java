package com.interviewintegrity.featureflag.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a per-user flag override.
 *
 * @param flagId owning flag
 * @param userId targeted user
 * @param variant variant to assign
 * @param enabled whether the flag is on for the user
 * @param addedAt instant the override was added
 */
public record FlagTargetResponse(
    UUID flagId, UUID userId, String variant, boolean enabled, Instant addedAt) {}
