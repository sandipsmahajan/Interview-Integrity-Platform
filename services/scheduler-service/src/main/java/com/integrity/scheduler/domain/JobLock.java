package com.integrity.scheduler.domain;

import java.time.Instant;
import java.util.UUID;

/** Handle returned when a distributed lock is acquired. */
public record JobLock(UUID jobId, String lockToken, String ownerId, Instant expiresAt) {}
