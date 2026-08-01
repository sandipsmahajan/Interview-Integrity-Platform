package com.interviewintegrity.storage.web.dto;

import com.interviewintegrity.storage.domain.StorageClass;
import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a storage object.
 *
 * @param id object identifier
 * @param organizationId owning tenant
 * @param bucketId owning bucket
 * @param key object key
 * @param sizeBytes payload size
 * @param contentType media type
 * @param checksumSha256 SHA-256 checksum of the payload
 * @param storageClass storage tier
 * @param storageRef object store reference
 * @param metadata JSON metadata document
 * @param uploadedBy user that uploaded the object
 * @param uploadedAt instant the object was uploaded
 */
public record ObjectResponse(
    UUID id,
    UUID organizationId,
    UUID bucketId,
    String key,
    long sizeBytes,
    String contentType,
    String checksumSha256,
    StorageClass storageClass,
    String storageRef,
    String metadata,
    UUID uploadedBy,
    Instant uploadedAt) {}
