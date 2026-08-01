package com.interviewintegrity.integration.web.dto;

import com.interviewintegrity.integration.domain.IntegrationStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Public view of an integration.
 *
 * @param id integration identifier
 * @param organizationId owning tenant
 * @param provider provider name
 * @param name display name
 * @param status lifecycle state
 * @param credentialsRef reference to the encrypted credential entry
 * @param config integration configuration
 * @param createdBy creating user
 * @param createdAt creation instant
 * @param updatedBy last modifying user
 * @param updatedAt last update instant
 */
public record IntegrationResponse(
    UUID id,
    UUID organizationId,
    String provider,
    String name,
    IntegrationStatus status,
    String credentialsRef,
    String config,
    UUID createdBy,
    Instant createdAt,
    UUID updatedBy,
    Instant updatedAt) {}
