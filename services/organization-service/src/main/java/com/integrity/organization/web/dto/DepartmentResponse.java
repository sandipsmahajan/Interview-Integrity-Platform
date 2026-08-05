package com.integrity.organization.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public profile of a department.
 *
 * @param id department identifier
 * @param organizationId owning tenant
 * @param parentId optional parent department
 * @param name department name
 * @param createdAt instant the department was created
 */
public record DepartmentResponse(
    UUID id, UUID organizationId, UUID parentId, String name, Instant createdAt) {}
