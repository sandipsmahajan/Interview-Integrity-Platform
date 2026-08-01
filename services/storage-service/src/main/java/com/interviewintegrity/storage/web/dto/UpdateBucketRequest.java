package com.interviewintegrity.storage.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to update a storage bucket.
 *
 * @param name bucket name
 * @param versioningEnabled whether object versions are retained
 * @param policy JSON policy document
 */
public record UpdateBucketRequest(
    @NotBlank @Size(max = 63) String name,
    Boolean versioningEnabled,
    @Size(max = 100000) String policy) {}
