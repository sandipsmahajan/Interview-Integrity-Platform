package com.interviewintegrity.notification.web.dto;

import com.interviewintegrity.notification.domain.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to set a notification preference.
 *
 * @param channel delivery channel
 * @param notificationType notification type code
 * @param enabled whether notifications are enabled
 */
public record SetPreferenceRequest(
    @NotNull NotificationChannel channel,
    @NotBlank @Size(max = 120) String notificationType,
    boolean enabled) {}
