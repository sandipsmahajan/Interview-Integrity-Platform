package com.interviewintegrity.notification.web.dto;

import com.interviewintegrity.notification.domain.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to create a notification template.
 *
 * @param code template code
 * @param channel delivery channel
 * @param subject subject line
 * @param bodyTemplate message body template
 * @param locale template locale
 */
public record CreateNotificationTemplateRequest(
    @NotBlank @Size(max = 120) String code,
    @NotNull NotificationChannel channel,
    @Size(max = 255) String subject,
    @NotBlank String bodyTemplate,
    @Size(max = 20) String locale) {}
