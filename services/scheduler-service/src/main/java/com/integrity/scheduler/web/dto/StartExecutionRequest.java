package com.integrity.scheduler.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request to start a job execution.
 *
 * @param jobId scheduled job
 * @param workerId executing worker
 */
public record StartExecutionRequest(
    @NotNull UUID jobId, @NotBlank @Size(max = 255) String workerId) {}
