package com.integrity.organization.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request to create an organization.
 *
 * @param name display name
 * @param slug unique URL slug, derived from the name when omitted
 * @param legalName registered legal name
 * @param settings JSON settings blob
 */
public record CreateOrganizationRequest(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 80)
        @Pattern(
            regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$",
            message = "slug must be lowercase words separated by hyphens")
        String slug,
    @Size(max = 160) String legalName,
    @Size(max = 8192) String settings) {}
