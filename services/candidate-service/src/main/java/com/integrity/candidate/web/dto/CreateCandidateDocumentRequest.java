package com.integrity.candidate.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request to register an uploaded document against a candidate.
 *
 * @param storageObjectId object id in the storage service
 * @param name display file name
 * @param contentType mime type of the object
 * @param sizeBytes size of the object in bytes
 */
public record CreateCandidateDocumentRequest(
    @NotNull UUID storageObjectId,
    @NotBlank @Size(max = 255) String name,
    @Size(max = 120) String contentType,
    @Min(0) long sizeBytes) {}
