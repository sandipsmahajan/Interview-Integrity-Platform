package com.integrity.scheduler.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public view of an acquired distributed lock.
 *
 * @param jobId locked job
 * @param lockToken release token
 * @param ownerId lock owner
 * @param expiresAt expiry instant
 */
public record JobLockResponse(UUID jobId, String lockToken, String ownerId, Instant expiresAt) {}
