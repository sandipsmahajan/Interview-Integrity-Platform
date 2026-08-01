package com.interviewintegrity.featureflag.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request to create an experiment.
 *
 * @param name display name
 * @param featureId targeted feature
 * @param controlVariant control variant
 * @param treatmentVariant treatment variant
 * @param metrics JSON metrics configuration
 */
public record CreateExperimentRequest(
    @NotBlank @Size(max = 150) String name,
    @NotNull UUID featureId,
    @Size(max = 200) String controlVariant,
    @Size(max = 200) String treatmentVariant,
    @Size(max = 100000) String metrics) {}
