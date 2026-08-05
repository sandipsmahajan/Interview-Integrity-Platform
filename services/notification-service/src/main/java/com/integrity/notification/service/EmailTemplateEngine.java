package com.integrity.notification.service;

import com.integrity.notification.domain.NotificationChannel;
import com.integrity.notification.domain.NotificationTemplate;
import com.integrity.notification.repository.NotificationTemplateRepository;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import reactor.core.publisher.Mono;

/**
 * Resolves and renders notification templates.
 *
 * <p>Template lookup prefers a tenant override, then the platform default for the requested locale
 * and finally the platform default English template. Placeholders written as {@code {{name}}} are
 * substituted from the supplied data map; a plaintext fallback is derived by stripping HTML tags.
 */
public final class EmailTemplateEngine {

  private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.]+)\\s*}}");
  private static final String DEFAULT_LOCALE = "en";

  private final NotificationTemplateRepository templateRepository;

  /** Creates an engine backed by the template repository. */
  public EmailTemplateEngine(NotificationTemplateRepository templateRepository) {
    this.templateRepository = templateRepository;
  }

  /**
   * Resolves and renders the best matching template for the notification type.
   *
   * @return the rendered subject and body, or empty when no template matches
   */
  public Mono<RenderedTemplate> render(
      UUID organizationId, String code, String locale, Map<String, String> data) {
    String resolvedLocale = locale == null || locale.isBlank() ? DEFAULT_LOCALE : locale;
    return resolve(organizationId, code, resolvedLocale)
        .map(template -> renderTemplate(template, data));
  }

  private Mono<NotificationTemplate> resolve(UUID organizationId, String code, String locale) {
    Mono<NotificationTemplate> tenant =
        organizationId == null
            ? Mono.empty()
            : templateRepository.findLiveByOrganizationCodeChannelLocale(
                organizationId, code, NotificationChannel.EMAIL, locale);
    Mono<NotificationTemplate> platformLocale =
        templateRepository.findLivePlatformDefault(code, NotificationChannel.EMAIL, locale);
    Mono<NotificationTemplate> platformDefault =
        templateRepository.findLivePlatformDefault(code, NotificationChannel.EMAIL, DEFAULT_LOCALE);
    return tenant.switchIfEmpty(platformLocale).switchIfEmpty(platformDefault);
  }

  private RenderedTemplate renderTemplate(NotificationTemplate template, Map<String, String> data) {
    String subject = substitute(template.getSubject(), data);
    String htmlBody = substitute(template.getBodyTemplate(), data);
    return new RenderedTemplate(subject, htmlBody, toPlainText(htmlBody));
  }

  static String substitute(String template, Map<String, String> data) {
    if (template == null || template.isBlank()) {
      return "";
    }
    Matcher matcher = PLACEHOLDER.matcher(template);
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      String key = matcher.group(1);
      String value = data == null ? "" : data.getOrDefault(key, "");
      matcher.appendReplacement(result, Matcher.quoteReplacement(value));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  /** Derives a plaintext fallback from an HTML fragment by stripping tags. */
  static String toPlainText(String html) {
    if (html == null) {
      return "";
    }
    String text = html.replaceAll("(?s)<style.*?</style>", " ");
    text = text.replaceAll("(?s)<[^>]+>", " ");
    text = text.replace("&nbsp;", " ");
    text = text.replace("&amp;", "&");
    text = text.replace("&lt;", "<");
    text = text.replace("&gt;", ">");
    text = text.replace("&#39;", "'");
    text = text.replace("&quot;", "\"");
    return text.replaceAll("\\s+", " ").trim();
  }

  /** A rendered email ready for dispatch. */
  public record RenderedTemplate(String subject, String htmlBody, String plainText) {}
}
