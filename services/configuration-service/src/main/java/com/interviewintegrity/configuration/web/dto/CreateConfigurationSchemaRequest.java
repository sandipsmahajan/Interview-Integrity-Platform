package com.interviewintegrity.configuration.web.dto;

import com.interviewintegrity.configuration.domain.ConfigValueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to declare a configuration key in the global catalog.
 *
 * @param key stable machine readable key
 * @param valueType allowed value type
 * @param defaultValue JSON default value
 * @param constraints JSON validation constraints
 * @param description human readable description
 */
public record CreateConfigurationSchemaRequest(
    @NotBlank @Size(max = 200) String key,
    @NotNull ConfigValueType valueType,
    @Size(max = 10000) String defaultValue,
    @Size(max = 10000) String constraints,
    @Size(max = 1000) String description) {}
