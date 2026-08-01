package com.interviewintegrity.featureflag.web.dto;

import com.interviewintegrity.featureflag.domain.FlagKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to create a feature.
 *
 * @param code stable machine readable code
 * @param name display name
 * @param description human readable description
 * @param kind value kind of the feature flag
 */
public record CreateFeatureRequest(
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 150) String name,
    @Size(max = 1000) String description,
    FlagKind kind) {}
