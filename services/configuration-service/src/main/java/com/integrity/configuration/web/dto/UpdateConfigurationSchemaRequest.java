package com.integrity.configuration.web.dto;

import com.integrity.configuration.domain.ConfigValueType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to update a configuration key declaration.
 *
 * @param valueType allowed value type
 * @param defaultValue JSON default value
 * @param constraints JSON validation constraints
 * @param description human readable description
 */
public record UpdateConfigurationSchemaRequest(
    @NotNull ConfigValueType valueType,
    @Size(max = 10000) String defaultValue,
    @Size(max = 10000) String constraints,
    @Size(max = 1000) String description) {}
