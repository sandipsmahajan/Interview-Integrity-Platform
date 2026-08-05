package com.integrity.scheduler.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to update a scheduled job.
 *
 * @param name display name
 * @param cronExpression cron expression (null for one-off jobs)
 * @param payload handler arguments
 * @param maxRetries retry budget
 * @param timeoutSeconds execution timeout
 */
public record UpdateScheduledJobRequest(
    @NotBlank @Size(max = 255) String name,
    @Size(max = 255) String cronExpression,
    @Size(max = 8000) String payload,
    @Min(0) @Max(100) int maxRetries,
    @Min(1) @Max(86400) int timeoutSeconds) {}
