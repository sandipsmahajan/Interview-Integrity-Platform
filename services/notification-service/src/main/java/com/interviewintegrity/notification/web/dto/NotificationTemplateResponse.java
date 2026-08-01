package com.interviewintegrity.notification.web.dto;

import com.interviewintegrity.notification.domain.NotificationChannel;
import java.time.Instant;
import java.util.UUID;

/**
 * Public view of a notification template.
 *
 * @param id template identifier
 * @param organizationId owning tenant (null for platform defaults)
 * @param code template code
 * @param channel delivery channel
 * @param subject subject line
 * @param bodyTemplate message body template
 * @param locale template locale
 * @param isDefault whether the template is the tenant default
 * @param createdAt creation instant
 * @param updatedAt last update instant
 */
public record NotificationTemplateResponse(
    UUID id,
    UUID organizationId,
    String code,
    NotificationChannel channel,
    String subject,
    String bodyTemplate,
    String locale,
    boolean isDefault,
    Instant createdAt,
    Instant updatedAt) {}
