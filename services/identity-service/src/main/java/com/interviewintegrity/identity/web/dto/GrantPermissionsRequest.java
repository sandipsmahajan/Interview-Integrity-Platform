package com.interviewintegrity.identity.web.dto;

import java.util.List;
import java.util.UUID;

/**
 * Request to grant permissions to a role.
 *
 * @param permissionIds permissions to grant
 */
public record GrantPermissionsRequest(List<UUID> permissionIds) {}
