package com.integrity.storage.web.dto;

import com.integrity.storage.domain.StorageClass;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request to register an object in a bucket.
 *
 * @param bucketId owning bucket
 * @param key object key
 * @param sizeBytes payload size
 * @param contentType media type
 * @param checksumSha256 SHA-256 checksum of the payload
 * @param storageClass storage tier
 * @param storageRef object store reference
 * @param metadata JSON metadata document
 */
public record RegisterObjectRequest(
    @NotNull UUID bucketId,
    @NotBlank @Size(max = 1024) String key,
    @NotNull @Min(0) Long sizeBytes,
    @Size(max = 255) String contentType,
    @Size(max = 64) String checksumSha256,
    StorageClass storageClass,
    @NotBlank @Size(max = 1024) String storageRef,
    @Size(max = 100000) String metadata) {}
