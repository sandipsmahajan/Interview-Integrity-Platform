package com.interviewintegrity.scheduler.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to create a scheduled job.
 *
 * @param name display name
 * @param jobType job type code
 * @param cronExpression cron expression (null for one-off jobs)
 * @param handler executable handler name
 * @param payload handler arguments
 * @param maxRetries retry budget
 * @param timeoutSeconds execution timeout
 */
public record CreateScheduledJobRequest(
    @NotBlank @Size(max = 255) String name,
    @NotBlank @Size(max = 120) String jobType,
    @Size(max = 255) String cronExpression,
    @NotBlank @Size(max = 255) String handler,
    @Size(max = 8000) String payload,
    @Min(0) @Max(100) int maxRetries,
    @Min(1) @Max(86400) int timeoutSeconds) {}
