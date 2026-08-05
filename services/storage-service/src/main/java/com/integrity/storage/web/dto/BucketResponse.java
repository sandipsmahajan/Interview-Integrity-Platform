package com.integrity.storage.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a storage bucket.
 *
 * @param id bucket identifier
 * @param organizationId owning tenant
 * @param name bucket name
 * @param versioningEnabled whether object versions are retained
 * @param policy JSON policy document
 * @param createdAt instant the bucket was created
 * @param updatedAt instant the bucket was last modified
 */
public record BucketResponse(
    UUID id,
    UUID organizationId,
    String name,
    boolean versioningEnabled,
    String policy,
    Instant createdAt,
    Instant updatedAt) {}
