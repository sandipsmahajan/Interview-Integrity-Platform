package com.interviewintegrity.integration.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to create an integration.
 *
 * @param provider provider name
 * @param name display name
 * @param credentialsRef reference to the encrypted credential entry
 * @param config integration configuration
 */
public record CreateIntegrationRequest(
    @NotBlank @Size(max = 120) String provider,
    @NotBlank @Size(max = 255) String name,
    @NotBlank @Size(max = 255) String credentialsRef,
    @Size(max = 8000) String config) {}
