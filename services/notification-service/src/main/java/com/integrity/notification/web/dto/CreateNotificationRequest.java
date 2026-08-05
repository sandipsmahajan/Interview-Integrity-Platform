package com.integrity.notification.web.dto;

import com.integrity.notification.domain.NotificationChannel;
import com.integrity.notification.domain.NotificationPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/**
 * Request to create a notification.
 *
 * @param userId recipient user
 * @param notificationType notification type code
 * @param channel delivery channel
 * @param subject subject line
 * @param body message body
 * @param priority urgency level
 * @param scheduledAt scheduled dispatch time
 */
public record CreateNotificationRequest(
    @NotNull UUID userId,
    @NotBlank @Size(max = 120) String notificationType,
    @NotNull NotificationChannel channel,
    @Size(max = 255) String subject,
    @NotBlank String body,
    @NotNull NotificationPriority priority,
    Instant scheduledAt) {}
