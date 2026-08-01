package com.interviewintegrity.integration.web.dto;

import com.interviewintegrity.integration.domain.IntegrationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public view of a connection to an external account.
 *
 * @param id connection identifier
 * @param organizationId owning tenant
 * @param integrationId parent integration
 * @param externalAccountId external account identifier
 * @param status lifecycle state
 * @param scopes granted scopes
 * @param connectedAt connection instant
 * @param lastSyncAt last synchronization instant
 * @param createdAt creation instant
 * @param updatedAt last update instant
 */
public record ConnectionResponse(
    UUID id,
    UUID organizationId,
    UUID integrationId,
    String externalAccountId,
    IntegrationStatus status,
    List<String> scopes,
    Instant connectedAt,
    Instant lastSyncAt,
    Instant createdAt,
    Instant updatedAt) {}
