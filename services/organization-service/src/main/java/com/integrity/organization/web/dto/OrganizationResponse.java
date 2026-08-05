package com.integrity.organization.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public profile of a tenant organization.
 *
 * @param id organization identifier
 * @param name display name
 * @param slug unique URL slug
 * @param legalName registered legal name
 * @param status lifecycle status
 * @param settings JSON settings blob
 * @param createdAt instant the organization was created
 * @param updatedAt instant of the last update
 */
public record OrganizationResponse(
    UUID id,
    String name,
    String slug,
    String legalName,
    String status,
    String settings,
    Instant createdAt,
    Instant updatedAt) {}
