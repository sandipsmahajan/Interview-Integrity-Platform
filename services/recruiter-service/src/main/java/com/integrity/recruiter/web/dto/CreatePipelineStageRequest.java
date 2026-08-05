package com.integrity.recruiter.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to create a pipeline stage.
 *
 * @param code stable machine readable code
 * @param name display name
 * @param orderIndex sort position within the pipeline
 */
public record CreatePipelineStageRequest(
    @NotBlank @Size(max = 60) String code,
    @NotBlank @Size(max = 150) String name,
    @Min(0) @Max(1000) int orderIndex) {}
