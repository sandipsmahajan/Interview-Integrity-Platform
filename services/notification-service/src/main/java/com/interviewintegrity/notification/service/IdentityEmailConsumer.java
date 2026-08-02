package com.interviewintegrity.notification.service;

import com.interviewintegrity.event.EventEnvelope;
import com.interviewintegrity.event.IdentityEmailEvent;
import com.interviewintegrity.event.KafkaTopics;
import com.interviewintegrity.notification.domain.NotificationChannel;
import com.interviewintegrity.notification.domain.NotificationPriority;
import com.interviewintegrity.notification.repository.NotificationPreferenceRepository;
import com.interviewintegrity.observability.MdcCorrelation;
import java.util.Locale;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import tools.jackson.databind.ObjectMapper;

/**
 * Consumes {@code identity.email.v1} events requesting email delivery.
 *
 * <p>Each event is filtered against the recipient's notification preferences, resolved to a tenant
 * or platform template, stored as a notification and dispatched immediately. Malformed messages are
 * logged and skipped; one failing message never stops the consumer.
 */
public final class IdentityEmailConsumer implements DisposableBean {

  private static final Logger log = LoggerFactory.getLogger(IdentityEmailConsumer.class);

  private final KafkaReceiver<String, String> receiver;
  private final EmailTemplateEngine templateEngine;
  private final EmailDispatchService dispatchService;
  private final NotificationService notificationService;
  private final NotificationPreferenceRepository preferenceRepository;
  private final ObjectMapper objectMapper;
  private volatile Disposable subscription;

  /** Creates a consumer bound to the given receiver and services. */
  public IdentityEmailConsumer(
      KafkaReceiver<String, String> receiver,
      EmailTemplateEngine templateEngine,
      EmailDispatchService dispatchService,
      NotificationService notificationService,
      NotificationPreferenceRepository preferenceRepository) {
    this.receiver = receiver;
    this.templateEngine = templateEngine;
    this.dispatchService = dispatchService;
    this.notificationService = notificationService;
    this.preferenceRepository = preferenceRepository;
    this.objectMapper = new ObjectMapper();
  }

  /** Subscribes to the identity email topic and starts processing records. */
  public void start() {
    subscription =
        receiver
            .receive()
            .subscribe(
                record ->
                    handle(record)
                        .doOnSuccess(ignored -> record.receiverOffset().acknowledge())
                        .subscribe(
                            ignored -> {},
                            error ->
                                log.error(
                                    "Failed to process email record at partition {} offset {}: {}",
                                    record.partition(),
                                    record.offset(),
                                    error.getMessage(),
                                    error)),
                error -> log.error("Email consumer terminated: {}", error.getMessage(), error));
    log.info("Email consumer subscribed to topic {}", KafkaTopics.IDENTITY_EMAIL);
  }

  @Override
  public void destroy() {
    Disposable active = subscription;
    if (active != null) {
      active.dispose();
    }
    log.info("Email consumer stopped");
  }

  Mono<Void> handle(ConsumerRecord<String, String> record) {
    try {
      EventEnvelope envelope = objectMapper.readValue(record.value(), EventEnvelope.class);
      IdentityEmailEvent event =
          objectMapper.readValue(envelope.payload(), IdentityEmailEvent.class);
      if (event.email() == null
          || event.email().isBlank()
          || event.notificationType() == null
          || event.notificationType().isBlank()) {
        if (log.isWarnEnabled()) {
          log.warn(
              "Skipping email message without recipient or type at partition {} offset {}",
              record.partition(),
              record.offset());
        }
        return Mono.empty();
      }
      return MdcCorrelation.withCorrelationId(
          isEnabled(event).filter(Boolean::booleanValue).flatMap(ignored -> process(event)),
          envelope.eventId().toString());
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error(
            "Skipping malformed email message at partition {} offset {}: {}",
            record.partition(),
            record.offset(),
            e.getMessage(),
            e);
      }
      return Mono.empty();
    }
  }

  private Mono<Boolean> isEnabled(IdentityEmailEvent event) {
    return preferenceRepository
        .findByUserChannelAndType(
            event.userId(), NotificationChannel.EMAIL, event.notificationType())
        .map(com.interviewintegrity.notification.domain.NotificationPreference::isEnabled)
        .defaultIfEmpty(true);
  }

  private Mono<Void> process(IdentityEmailEvent event) {
    return templateEngine
        .render(
            event.organizationId(),
            event.notificationType(),
            locale(event.locale()),
            event.templateData())
        .flatMap(
            rendered ->
                notificationService
                    .createEmailNotification(
                        event.organizationId(),
                        event.userId(),
                        event.email(),
                        event.notificationType(),
                        rendered.subject(),
                        rendered.htmlBody(),
                        NotificationPriority.MEDIUM,
                        null)
                    .flatMap(notification -> dispatchService.dispatch(notification)))
        .switchIfEmpty(
            Mono.fromRunnable(
                () -> {
                  if (log.isWarnEnabled()) {
                    log.warn(
                        "No email template for type '{}' and locale '{}'; skipping",
                        event.notificationType(),
                        event.locale());
                  }
                }));
  }

  private static String locale(String raw) {
    if (raw == null || raw.isBlank()) {
      return Locale.ROOT.toLanguageTag();
    }
    return raw;
  }
}
