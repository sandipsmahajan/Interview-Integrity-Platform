package com.interviewintegrity.notification.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to update a notification template.
 *
 * @param subject subject line
 * @param bodyTemplate message body template
 * @param locale template locale
 */
public record UpdateNotificationTemplateRequest(
    @Size(max = 255) String subject,
    @NotBlank String bodyTemplate,
    @Size(max = 20) String locale) {}
