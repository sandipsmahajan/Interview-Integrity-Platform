package com.integrity.organization.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to update the mutable profile of an organization.
 *
 * @param name display name
 * @param legalName registered legal name
 * @param settings JSON settings blob
 */
public record UpdateOrganizationRequest(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 160) String legalName,
    @Size(max = 8192) String settings) {}
