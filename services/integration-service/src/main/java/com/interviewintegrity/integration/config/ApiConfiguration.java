package com.interviewintegrity.integration.config;

import com.interviewintegrity.integration.service.IntegrationConnectionService;
import com.interviewintegrity.integration.service.IntegrationService;
import com.interviewintegrity.integration.service.IntegrationSyncLogService;
import com.interviewintegrity.integration.service.IntegrationWebhookService;
import com.interviewintegrity.integration.web.IntegrationConnectionController;
import com.interviewintegrity.integration.web.IntegrationController;
import com.interviewintegrity.integration.web.IntegrationSyncLogController;
import com.interviewintegrity.integration.web.IntegrationWebhookController;
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

  /** Exposes the integration controller. */
  @Bean
  public IntegrationController integrationController(IntegrationService integrationService) {
    return new IntegrationController(integrationService);
  }

  /** Exposes the connection controller. */
  @Bean
  public IntegrationConnectionController integrationConnectionController(
      IntegrationConnectionService connectionService) {
    return new IntegrationConnectionController(connectionService);
  }

  /** Exposes the webhook controller. */
  @Bean
  public IntegrationWebhookController integrationWebhookController(
      IntegrationWebhookService webhookService) {
    return new IntegrationWebhookController(webhookService);
  }

  /** Exposes the sync log controller. */
  @Bean
  public IntegrationSyncLogController integrationSyncLogController(
      IntegrationSyncLogService syncLogService) {
    return new IntegrationSyncLogController(syncLogService);
  }

  /** Describes the OpenAPI document for the integration service. */
  @Bean
  public OpenAPI platformOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Integration Service API")
                .version("v1")
                .description(
                    "External integrations, connections, webhooks and synchronization logs"))
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
