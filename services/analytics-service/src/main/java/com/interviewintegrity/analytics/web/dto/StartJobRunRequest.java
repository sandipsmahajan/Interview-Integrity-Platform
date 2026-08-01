package com.interviewintegrity.analytics.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to start an analytics job run.
 *
 * @param jobName name of the aggregation job
 */
public record StartJobRunRequest(@NotBlank @Size(max = 200) String jobName) {}
