package com.integrity.identity.web.dto;

import java.util.List;
import java.util.UUID;

/**
 * Request to assign roles to a user.
 *
 * @param roleIds roles to assign
 */
public record AssignRolesRequest(List<UUID> roleIds) {}
