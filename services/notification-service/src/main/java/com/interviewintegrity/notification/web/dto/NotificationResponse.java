package com.interviewintegrity.notification.web.dto;

import com.interviewintegrity.notification.domain.NotificationChannel;
import com.interviewintegrity.notification.domain.NotificationPriority;
import com.interviewintegrity.notification.domain.NotificationStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Public view of a notification.
 *
 * @param id notification identifier
 * @param organizationId owning tenant
 * @param userId recipient user
 * @param notificationType notification type code
 * @param channel delivery channel
 * @param subject subject line
 * @param body message body
 * @param priority urgency level
 * @param status lifecycle state
 * @param scheduledAt scheduled dispatch time
 * @param sentAt dispatch time
 * @param readAt read time
 * @param createdAt creation instant
 * @param updatedAt last update instant
 */
public record NotificationResponse(
    UUID id,
    UUID organizationId,
    UUID userId,
    String notificationType,
    NotificationChannel channel,
    String subject,
    String body,
    NotificationPriority priority,
    NotificationStatus status,
    Instant scheduledAt,
    Instant sentAt,
    Instant readAt,
    Instant createdAt,
    Instant updatedAt) {}
