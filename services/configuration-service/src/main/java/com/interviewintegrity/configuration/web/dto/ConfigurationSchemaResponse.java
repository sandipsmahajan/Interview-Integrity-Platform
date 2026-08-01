package com.interviewintegrity.configuration.web.dto;

import com.interviewintegrity.configuration.domain.ConfigValueType;
import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a configuration schema entry.
 *
 * @param id schema identifier
 * @param key stable machine readable key
 * @param valueType allowed value type
 * @param defaultValue JSON default value
 * @param constraints JSON validation constraints
 * @param description human readable description
 * @param createdAt instant the entry was declared
 */
public record ConfigurationSchemaResponse(
    UUID id,
    String key,
    ConfigValueType valueType,
    String defaultValue,
    String constraints,
    String description,
    Instant createdAt) {}
