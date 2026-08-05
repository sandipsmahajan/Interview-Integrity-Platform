package com.integrity.organization.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request to claim an email domain for an organization.
 *
 * @param domain domain name to claim, e.g. {@code example.com}
 */
public record AddDomainRequest(
    @NotBlank
        @Size(max = 200)
        @Pattern(
            regexp = "^[a-z0-9.-]+\\.[a-z]{2,}$",
            message = "domain must look like example.com")
        String domain) {}
