package com.integrity.notification.config;

import com.integrity.notification.service.NotificationMapper;
import com.integrity.notification.service.NotificationPreferenceService;
import com.integrity.notification.service.NotificationService;
import com.integrity.notification.service.NotificationTemplateService;
import com.integrity.notification.web.NotificationController;
import com.integrity.notification.web.NotificationPreferenceController;
import com.integrity.notification.web.NotificationTemplateController;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the REST controllers as beans and describes the OpenAPI surface of the service. */
@Configuration
public class ApiConfiguration {

  /** Exposes the notification controller. */
  @Bean
  public NotificationController notificationController(
      NotificationService notificationService, NotificationMapper mapper) {
    return new NotificationController(notificationService, mapper);
  }

  /** Exposes the notification template controller. */
  @Bean
  public NotificationTemplateController notificationTemplateController(
      NotificationTemplateService templateService, NotificationMapper mapper) {
    return new NotificationTemplateController(templateService, mapper);
  }

  /** Exposes the notification preference controller. */
  @Bean
  public NotificationPreferenceController notificationPreferenceController(
      NotificationPreferenceService preferenceService, NotificationMapper mapper) {
    return new NotificationPreferenceController(preferenceService, mapper);
  }

  /** Describes the OpenAPI document for the notification service. */
  @Bean
  public OpenAPI platformOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Notification Service API")
                .version("v1")
                .description(
                    "Notification creation, templates, preferences and delivery lifecycle"))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
