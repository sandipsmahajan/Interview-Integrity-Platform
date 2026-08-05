package com.integrity.identity.web.dto;

import java.util.UUID;

/**
 * Public representation of a permission code.
 *
 * @param id permission identifier
 * @param code stable permission code
 * @param name display name
 * @param description description
 */
public record PermissionResponse(UUID id, String code, String name, String description) {}
