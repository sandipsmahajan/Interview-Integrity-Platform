package com.integrity.identity.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public representation of a role.
 *
 * @param id role identifier
 * @param organizationId tenant identifier
 * @param code stable role code
 * @param name display name
 * @param description description
 * @param system true for system seeded roles
 * @param createdAt creation instant
 * @param permissionCodes permission codes granted to the role
 */
public record RoleResponse(
    UUID id,
    UUID organizationId,
    String code,
    String name,
    String description,
    boolean system,
    Instant createdAt,
    List<String> permissionCodes) {}
