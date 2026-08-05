package com.integrity.storage.web.dto;

import com.integrity.storage.domain.StorageClass;
import jakarta.validation.constraints.Size;

/**
 * Request to update an object's mutable metadata.
 *
 * @param contentType media type
 * @param storageClass storage tier
 * @param metadata JSON metadata document
 */
public record UpdateObjectRequest(
    @Size(max = 255) String contentType,
    StorageClass storageClass,
    @Size(max = 100000) String metadata) {}
