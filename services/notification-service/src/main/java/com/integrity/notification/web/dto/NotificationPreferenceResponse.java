package com.integrity.notification.web.dto;

import com.integrity.notification.domain.NotificationChannel;
import java.time.Instant;
import java.util.UUID;

/**
 * Public view of a notification preference.
 *
 * @param id preference identifier
 * @param organizationId owning tenant
 * @param userId user
 * @param channel delivery channel
 * @param notificationType notification type code
 * @param enabled whether notifications are enabled
 * @param createdAt creation instant
 * @param updatedAt last update instant
 */
public record NotificationPreferenceResponse(
    UUID id,
    UUID organizationId,
    UUID userId,
    NotificationChannel channel,
    String notificationType,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt) {}
