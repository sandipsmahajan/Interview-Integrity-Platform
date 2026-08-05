package com.integrity.configuration.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a configuration version record.
 *
 * @param id history identifier
 * @param configurationId owning configuration
 * @param organizationId owning tenant
 * @param key configuration key
 * @param oldValue previous JSON value
 * @param newValue new JSON value
 * @param changedBy user that applied the change
 * @param changedAt instant the change was applied
 * @param version configuration version after the change
 */
public record ConfigurationHistoryResponse(
    Long id,
    UUID configurationId,
    UUID organizationId,
    String key,
    String oldValue,
    String newValue,
    UUID changedBy,
    Instant changedAt,
    long version) {}
