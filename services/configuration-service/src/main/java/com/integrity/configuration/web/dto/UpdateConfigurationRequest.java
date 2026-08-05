package com.integrity.configuration.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to update a tenant scoped configuration value.
 *
 * @param value JSON value
 * @param description human readable description
 */
public record UpdateConfigurationRequest(
    @NotBlank @Size(max = 10000) String value, @Size(max = 1000) String description) {}
