package com.interviewintegrity.scheduler.web.dto;

import com.interviewintegrity.scheduler.domain.ExecutionStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Public view of a job execution.
 *
 * @param id execution identifier
 * @param organizationId owning tenant
 * @param jobId parent job
 * @param status lifecycle state
 * @param startedAt start instant
 * @param finishedAt finish instant
 * @param exitCode process exit code
 * @param errorMessage failure detail
 * @param durationMs run duration in milliseconds
 * @param workerId executing worker
 * @param createdAt creation instant
 */
public record JobExecutionResponse(
    UUID id,
    UUID organizationId,
    UUID jobId,
    ExecutionStatus status,
    Instant startedAt,
    Instant finishedAt,
    Integer exitCode,
    String errorMessage,
    Long durationMs,
    String workerId,
    Instant createdAt) {}
