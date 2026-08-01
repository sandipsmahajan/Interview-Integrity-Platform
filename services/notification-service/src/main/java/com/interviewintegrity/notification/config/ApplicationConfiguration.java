package com.interviewintegrity.notification.config;

import com.interviewintegrity.notification.repository.NotificationDeliveryRepository;
import com.interviewintegrity.notification.repository.NotificationPreferenceRepository;
import com.interviewintegrity.notification.repository.NotificationRepository;
import com.interviewintegrity.notification.repository.NotificationTemplateRepository;
import com.interviewintegrity.notification.service.EmailDispatchService;
import com.interviewintegrity.notification.service.EmailDispatcher;
import com.interviewintegrity.notification.service.EmailRetryWorker;
import com.interviewintegrity.notification.service.EmailTemplateEngine;
import com.interviewintegrity.notification.service.IdentityEmailConsumer;
import com.interviewintegrity.notification.service.NotificationPreferenceService;
import com.interviewintegrity.notification.service.NotificationService;
import com.interviewintegrity.notification.service.NotificationTemplateService;
import com.interviewintegrity.notification.service.SmtpEmailDispatcher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import reactor.kafka.receiver.KafkaReceiver;

/**
 * Explicit bean wiring for the notification service application services.
 *
 * <p>Services are plain classes instantiated here rather than discovered by component scanning,
 * keeping the dependency graph visible in one place.
 */
@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class ApplicationConfiguration {

  /** Provides the notification service. */
  @Bean
  public NotificationService notificationService(
      NotificationRepository notificationRepository,
      NotificationDeliveryRepository deliveryRepository) {
    return new NotificationService(notificationRepository, deliveryRepository);
  }

  /** Provides the notification template service. */
  @Bean
  public NotificationTemplateService notificationTemplateService(
      NotificationTemplateRepository templateRepository) {
    return new NotificationTemplateService(templateRepository);
  }

  /** Provides the notification preference service. */
  @Bean
  public NotificationPreferenceService notificationPreferenceService(
      NotificationPreferenceRepository preferenceRepository) {
    return new NotificationPreferenceService(preferenceRepository);
  }

  /** Provides the email template resolver and renderer. */
  @Bean
  public EmailTemplateEngine emailTemplateEngine(
      NotificationTemplateRepository templateRepository) {
    return new EmailTemplateEngine(templateRepository);
  }

  /** Provides the SMTP backed email dispatcher. */
  @Bean
  public EmailDispatcher emailDispatcher(JavaMailSender mailSender, MailProperties mailProperties) {
    return new SmtpEmailDispatcher(mailSender, mailProperties);
  }

  /** Provides the email dispatch coordinator. */
  @Bean
  public EmailDispatchService emailDispatchService(
      NotificationService notificationService,
      EmailDispatcher emailDispatcher,
      MailProperties mailProperties) {
    return new EmailDispatchService(notificationService, emailDispatcher, mailProperties);
  }

  /** Starts the consumer of identity email events. */
  @Bean
  public IdentityEmailConsumer identityEmailConsumer(
      KafkaReceiver<String, String> emailReceiver,
      EmailTemplateEngine emailTemplateEngine,
      EmailDispatchService emailDispatchService,
      NotificationService notificationService,
      NotificationPreferenceRepository preferenceRepository) {
    IdentityEmailConsumer consumer =
        new IdentityEmailConsumer(
            emailReceiver,
            emailTemplateEngine,
            emailDispatchService,
            notificationService,
            preferenceRepository);
    consumer.start();
    return consumer;
  }

  /** Starts the periodic email retry worker. */
  @Bean
  public EmailRetryWorker emailRetryWorker(
      EmailDispatchService emailDispatchService, MailProperties mailProperties) {
    EmailRetryWorker worker = new EmailRetryWorker(emailDispatchService, mailProperties);
    worker.start();
    return worker;
  }
}
