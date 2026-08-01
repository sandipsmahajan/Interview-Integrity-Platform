package com.interviewintegrity.notification.config;

import com.interviewintegrity.notification.repository.NotificationDeliveryRepository;
import com.interviewintegrity.notification.repository.NotificationPreferenceRepository;
import com.interviewintegrity.notification.repository.NotificationRepository;
import com.interviewintegrity.notification.repository.NotificationTemplateRepository;
import com.interviewintegrity.notification.service.NotificationPreferenceService;
import com.interviewintegrity.notification.service.NotificationService;
import com.interviewintegrity.notification.service.NotificationTemplateService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit bean wiring for the notification service application services.
 *
 * <p>Services are plain classes instantiated here rather than discovered by component scanning,
 * keeping the dependency graph visible in one place.
 */
@Configuration
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
}
