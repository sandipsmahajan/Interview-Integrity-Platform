package com.integrity.candidate.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a candidate document.
 *
 * @param id document identifier
 * @param candidateId owning candidate
 * @param storageObjectId object id in the storage service
 * @param name display file name
 * @param contentType mime type of the object
 * @param sizeBytes size of the object in bytes
 * @param uploadedBy user that uploaded the document
 * @param uploadedAt instant the document was uploaded
 */
public record CandidateDocumentResponse(
    UUID id,
    UUID candidateId,
    UUID storageObjectId,
    String name,
    String contentType,
    long sizeBytes,
    UUID uploadedBy,
    Instant uploadedAt) {}
