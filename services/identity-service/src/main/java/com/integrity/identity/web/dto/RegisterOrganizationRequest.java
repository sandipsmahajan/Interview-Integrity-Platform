package com.integrity.identity.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to register a new organization with its first administrator.
 *
 * @param companyName name of the new organization
 * @param adminEmail email of the initial administrator
 * @param adminPassword initial password of the administrator
 * @param adminDisplayName display name of the administrator
 */
public record RegisterOrganizationRequest(
    @NotBlank @Size(max = 120) String companyName,
    @NotBlank @Email @Size(max = 320) String adminEmail,
    @NotBlank @Size(min = 8, max = 128) String adminPassword,
    @NotBlank @Size(max = 120) String adminDisplayName) {}
