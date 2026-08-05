package com.integrity.featureflag.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request to add a per-user flag override.
 *
 * @param userId targeted user
 * @param variant variant to assign
 * @param enabled whether the flag is on for the user
 */
public record AddTargetRequest(
    @NotNull UUID userId, @Size(max = 200) String variant, boolean enabled) {}
