package com.integrity.scheduler.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to release a distributed lock.
 *
 * @param lockToken token returned by the acquire call
 */
public record ReleaseLockRequest(@NotBlank @Size(max = 255) String lockToken) {}
