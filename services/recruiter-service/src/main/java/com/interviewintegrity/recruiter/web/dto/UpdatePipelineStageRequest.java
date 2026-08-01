package com.interviewintegrity.recruiter.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to update a pipeline stage.
 *
 * @param name display name
 * @param orderIndex sort position within the pipeline
 */
public record UpdatePipelineStageRequest(
    @NotBlank @Size(max = 150) String name, @Min(0) @Max(1000) int orderIndex) {}
