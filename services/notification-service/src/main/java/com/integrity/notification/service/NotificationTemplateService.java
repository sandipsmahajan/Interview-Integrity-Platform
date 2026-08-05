package com.integrity.notification.service;

import com.integrity.exception.ConflictException;
import com.integrity.exception.NotFoundException;
import com.integrity.notification.domain.NotificationChannel;
import com.integrity.notification.domain.NotificationTemplate;
import com.integrity.notification.repository.NotificationTemplateRepository;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages the message templates of an organization and the platform defaults. */
public class NotificationTemplateService {

  private final NotificationTemplateRepository templateRepository;

  /** Wires the service with its repository. */
  public NotificationTemplateService(NotificationTemplateRepository templateRepository) {
    this.templateRepository = templateRepository;
  }

  /** Creates a tenant template, rejecting a duplicate for code, channel and locale. */
  @Transactional
  public Mono<NotificationTemplate> createTemplate(
      UUID organizationId,
      String code,
      NotificationChannel channel,
      String subject,
      String bodyTemplate,
      String locale) {
    String resolvedLocale = resolveLocale(locale);
    return templateRepository
        .findLiveByOrganizationCodeChannelLocale(organizationId, code, channel, resolvedLocale)
        .hasElement()
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(
                    new ConflictException("Template already exists for code, channel and locale"));
              }
              return templateRepository.save(
                  new NotificationTemplate(
                      organizationId, code, channel, subject, bodyTemplate, resolvedLocale));
            });
  }

  /** Returns a single live template of the organization. */
  @Transactional(readOnly = true)
  public Mono<NotificationTemplate> getTemplate(UUID templateId, UUID organizationId) {
    return templateRepository
        .findLiveById(templateId)
        .switchIfEmpty(Mono.error(new NotFoundException("Notification template not found")))
        .flatMap(
            template -> {
              if (!organizationId.equals(template.getOrganizationId())) {
                return Mono.error(new NotFoundException("Notification template not found"));
              }
              return Mono.just(template);
            });
  }

  /** Lists the live templates of an organization. */
  @Transactional(readOnly = true)
  public Flux<NotificationTemplate> listTemplates(UUID organizationId) {
    return templateRepository.listLiveByOrganization(organizationId);
  }

  /** Updates a template body and metadata. */
  @Transactional
  public Mono<NotificationTemplate> updateTemplate(
      UUID templateId, UUID organizationId, String subject, String bodyTemplate, String locale) {
    return getTemplate(templateId, organizationId)
        .map(
            template -> {
              template.update(subject, bodyTemplate, locale);
              return template;
            })
        .flatMap(templateRepository::save);
  }

  /** Marks a template as the tenant default for its code and channel. */
  @Transactional
  public Mono<NotificationTemplate> setDefault(UUID templateId, UUID organizationId) {
    return getTemplate(templateId, organizationId)
        .map(
            template -> {
              template.setDefault(true);
              return template;
            })
        .flatMap(templateRepository::save);
  }

  /** Soft deletes a template. */
  @Transactional
  public Mono<Void> deleteTemplate(UUID templateId, UUID organizationId) {
    return getTemplate(templateId, organizationId)
        .flatMap(
            template -> {
              template.delete();
              return templateRepository.save(template).then();
            });
  }

  private static String resolveLocale(String locale) {
    return locale == null || locale.isBlank() ? "en" : locale;
  }
}
