package com.integrity.scheduler.web.dto;

import jakarta.validation.constraints.Size;

/**
 * Request to finish a job execution.
 *
 * @param exitCode process exit code
 * @param errorMessage failure detail
 */
public record FinishExecutionRequest(int exitCode, @Size(max = 2000) String errorMessage) {}
