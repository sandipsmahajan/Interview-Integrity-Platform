package com.integrity.candidate.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a tag.
 *
 * @param id tag identifier
 * @param organizationId owning tenant
 * @param code stable machine readable code
 * @param name display name
 * @param createdAt instant the tag was created
 */
public record TagResponse(
    UUID id, UUID organizationId, String code, String name, Instant createdAt) {}
