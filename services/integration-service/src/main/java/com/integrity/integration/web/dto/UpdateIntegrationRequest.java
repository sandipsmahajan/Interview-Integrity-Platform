package com.integrity.integration.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to update an integration.
 *
 * @param name display name
 * @param config integration configuration
 */
public record UpdateIntegrationRequest(
    @NotBlank @Size(max = 255) String name, @Size(max = 8000) String config) {}
