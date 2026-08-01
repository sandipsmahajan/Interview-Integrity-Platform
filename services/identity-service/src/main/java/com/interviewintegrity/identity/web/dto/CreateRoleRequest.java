package com.interviewintegrity.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to create a role.
 *
 * @param code stable role code, uppercase snake case
 * @param name display name
 * @param description description
 */
public record CreateRoleRequest(
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 120) String name,
    @Size(max = 500) String description) {}
