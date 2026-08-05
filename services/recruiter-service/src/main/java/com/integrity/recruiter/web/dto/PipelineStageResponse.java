package com.integrity.recruiter.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public profile of a pipeline stage.
 *
 * @param id stage identifier
 * @param organizationId owning tenant
 * @param code stable machine readable code
 * @param name display name
 * @param orderIndex sort position within the pipeline
 * @param createdAt instant the stage was created
 */
public record PipelineStageResponse(
    UUID id, UUID organizationId, String code, String name, int orderIndex, Instant createdAt) {}
