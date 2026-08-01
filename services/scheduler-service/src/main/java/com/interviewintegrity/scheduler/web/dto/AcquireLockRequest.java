package com.interviewintegrity.scheduler.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to acquire a distributed lock.
 *
 * @param ownerId lock owner identifier
 * @param ttlSeconds lock validity window
 */
public record AcquireLockRequest(
    @NotBlank @Size(max = 255) String ownerId, @Min(1) long ttlSeconds) {}
