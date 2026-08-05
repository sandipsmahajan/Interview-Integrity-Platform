package com.integrity.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Published when the identity service wants an email delivered to a user.
 *
 * <p>The notification service consumes this event, resolves the tenant or platform default template
 * for {@code notificationType} and {@code locale}, renders the subject and body with {@code
 * templateData}, and dispatches it through the configured mail channel.
 *
 * @param userId recipient user identifier
 * @param organizationId owning organization identifier
 * @param email recipient email address
 * @param displayName recipient display name, used in the greeting
 * @param locale IETF language tag for template selection, defaults to {@code en}
 * @param notificationType template code identifying the email, e.g. {@code email-verification}
 * @param templateData values substituted into the template placeholders
 * @param occurredAt instant the email was requested
 */
public record IdentityEmailEvent(
    UUID userId,
    UUID organizationId,
    String email,
    String displayName,
    String locale,
    String notificationType,
    Map<String, String> templateData,
    Instant occurredAt) {}
