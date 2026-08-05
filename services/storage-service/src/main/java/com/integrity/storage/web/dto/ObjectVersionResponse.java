package com.integrity.storage.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of an object version.
 *
 * @param id version identifier
 * @param objectId owning object
 * @param version sequential version number
 * @param storageRef object store reference
 * @param sizeBytes payload size
 * @param checksumSha256 SHA-256 checksum of the payload
 * @param createdBy user that created the version
 * @param createdAt instant the version was created
 */
public record ObjectVersionResponse(
    Long id,
    UUID objectId,
    int version,
    String storageRef,
    long sizeBytes,
    String checksumSha256,
    UUID createdBy,
    Instant createdAt) {}
