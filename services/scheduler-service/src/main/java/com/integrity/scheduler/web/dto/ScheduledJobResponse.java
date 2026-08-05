package com.integrity.scheduler.web.dto;

import com.integrity.scheduler.domain.ExecutionStatus;
import com.integrity.scheduler.domain.JobStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Public view of a scheduled job.
 *
 * @param id job identifier
 * @param organizationId owning tenant
 * @param name display name
 * @param jobType job type code
 * @param cronExpression cron expression
 * @param handler executable handler name
 * @param payload handler arguments
 * @param status lifecycle state
 * @param maxRetries retry budget
 * @param timeoutSeconds execution timeout
 * @param retryCount current retry count
 * @param lastRunAt last run instant
 * @param lastRunStatus last run state
 * @param nextRunAt next scheduled run
 * @param createdBy creating user
 * @param createdAt creation instant
 * @param updatedBy last modifying user
 * @param updatedAt last update instant
 */
public record ScheduledJobResponse(
    UUID id,
    UUID organizationId,
    String name,
    String jobType,
    String cronExpression,
    String handler,
    String payload,
    JobStatus status,
    int maxRetries,
    int timeoutSeconds,
    int retryCount,
    Instant lastRunAt,
    ExecutionStatus lastRunStatus,
    Instant nextRunAt,
    UUID createdBy,
    Instant createdAt,
    UUID updatedBy,
    Instant updatedAt) {}
