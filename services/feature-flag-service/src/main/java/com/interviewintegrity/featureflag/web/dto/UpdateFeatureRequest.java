package com.interviewintegrity.featureflag.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to update a feature.
 *
 * @param name display name
 * @param description human readable description
 */
public record UpdateFeatureRequest(
    @NotBlank @Size(max = 150) String name, @Size(max = 1000) String description) {}
