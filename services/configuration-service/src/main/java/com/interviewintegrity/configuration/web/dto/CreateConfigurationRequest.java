package com.interviewintegrity.configuration.web.dto;

import com.interviewintegrity.configuration.domain.ConfigScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to create a tenant scoped configuration value.
 *
 * @param scope visibility scope
 * @param key configuration key
 * @param value JSON value
 * @param description human readable description
 */
public record CreateConfigurationRequest(
    @NotNull ConfigScope scope,
    @NotBlank @Size(max = 200) String key,
    @NotBlank @Size(max = 10000) String value,
    @Size(max = 1000) String description) {}
