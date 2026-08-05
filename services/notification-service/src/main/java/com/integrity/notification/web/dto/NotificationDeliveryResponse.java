package com.integrity.notification.web.dto;

import com.integrity.notification.domain.NotificationChannel;
import com.integrity.notification.domain.NotificationStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Public view of a delivery attempt.
 *
 * @param id delivery identifier
 * @param notificationId parent notification
 * @param channel delivery channel
 * @param provider provider name
 * @param providerMessageId provider message identifier
 * @param status delivery state
 * @param attempts attempt counter
 * @param lastError failure detail
 * @param sentAt dispatch time
 * @param createdAt creation instant
 */
public record NotificationDeliveryResponse(
    Long id,
    UUID notificationId,
    NotificationChannel channel,
    String provider,
    String providerMessageId,
    NotificationStatus status,
    int attempts,
    String lastError,
    Instant sentAt,
    Instant createdAt) {}
