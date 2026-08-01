package com.interviewintegrity.configuration.web.dto;

import com.interviewintegrity.configuration.domain.ConfigScope;
import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a tenant scoped configuration value.
 *
 * @param id configuration identifier
 * @param organizationId owning tenant
 * @param scope visibility scope
 * @param key configuration key
 * @param value JSON value
 * @param description human readable description
 * @param createdAt instant the value was created
 * @param updatedAt instant the value was last modified
 */
public record ConfigurationResponse(
    UUID id,
    UUID organizationId,
    ConfigScope scope,
    String key,
    String value,
    String description,
    Instant createdAt,
    Instant updatedAt) {}
