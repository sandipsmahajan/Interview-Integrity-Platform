package com.interviewintegrity.integration.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public view of a webhook subscription.
 *
 * @param id webhook identifier
 * @param organizationId owning tenant
 * @param integrationId parent integration
 * @param url delivery endpoint
 * @param events subscribed events
 * @param enabled whether delivery is active
 * @param createdAt creation instant
 * @param updatedAt last update instant
 */
public record WebhookResponse(
    UUID id,
    UUID organizationId,
    UUID integrationId,
    String url,
    List<String> events,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt) {}
