package com.integrity.integration.config;

import com.integrity.integration.service.IntegrationConnectionService;
import com.integrity.integration.service.IntegrationMapper;
import com.integrity.integration.service.IntegrationService;
import com.integrity.integration.service.IntegrationSyncLogService;
import com.integrity.integration.service.IntegrationWebhookService;
import com.integrity.integration.web.IntegrationConnectionController;
import com.integrity.integration.web.IntegrationController;
import com.integrity.integration.web.IntegrationSyncLogController;
import com.integrity.integration.web.IntegrationWebhookController;
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
  public IntegrationController integrationController(
      IntegrationService integrationService, IntegrationMapper mapper) {
    return new IntegrationController(integrationService, mapper);
  }

  /** Exposes the connection controller. */
  @Bean
  public IntegrationConnectionController integrationConnectionController(
      IntegrationConnectionService connectionService, IntegrationMapper mapper) {
    return new IntegrationConnectionController(connectionService, mapper);
  }

  /** Exposes the webhook controller. */
  @Bean
  public IntegrationWebhookController integrationWebhookController(
      IntegrationWebhookService webhookService, IntegrationMapper mapper) {
    return new IntegrationWebhookController(webhookService, mapper);
  }

  /** Exposes the sync log controller. */
  @Bean
  public IntegrationSyncLogController integrationSyncLogController(
      IntegrationSyncLogService syncLogService, IntegrationMapper mapper) {
    return new IntegrationSyncLogController(syncLogService, mapper);
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
