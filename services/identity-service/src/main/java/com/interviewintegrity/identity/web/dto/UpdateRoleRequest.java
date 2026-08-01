package com.interviewintegrity.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to update the mutable fields of a role.
 *
 * @param name display name
 * @param description description
 */
public record UpdateRoleRequest(
    @NotBlank @Size(max = 120) String name, @Size(max = 500) String description) {}
