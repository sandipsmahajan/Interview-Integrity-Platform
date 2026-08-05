package com.integrity.integration.config;

import com.integrity.integration.repository.IntegrationConnectionRepository;
import com.integrity.integration.repository.IntegrationRepository;
import com.integrity.integration.repository.IntegrationSyncLogRepository;
import com.integrity.integration.repository.IntegrationWebhookRepository;
import com.integrity.integration.service.IntegrationConnectionService;
import com.integrity.integration.service.IntegrationService;
import com.integrity.integration.service.IntegrationSyncLogService;
import com.integrity.integration.service.IntegrationWebhookService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit bean wiring for the integration service application services.
 *
 * <p>Services are plain classes instantiated here rather than discovered by component scanning,
 * keeping the dependency graph visible in one place.
 */
@Configuration
public class ApplicationConfiguration {

  /** Provides the integration service. */
  @Bean
  public IntegrationService integrationService(IntegrationRepository integrationRepository) {
    return new IntegrationService(integrationRepository);
  }

  /** Provides the connection service. */
  @Bean
  public IntegrationConnectionService integrationConnectionService(
      IntegrationConnectionRepository connectionRepository,
      IntegrationRepository integrationRepository) {
    return new IntegrationConnectionService(connectionRepository, integrationRepository);
  }

  /** Provides the webhook service. */
  @Bean
  public IntegrationWebhookService integrationWebhookService(
      IntegrationWebhookRepository webhookRepository, IntegrationRepository integrationRepository) {
    return new IntegrationWebhookService(webhookRepository, integrationRepository);
  }

  /** Provides the sync log service. */
  @Bean
  public IntegrationSyncLogService integrationSyncLogService(
      IntegrationSyncLogRepository syncLogRepository,
      IntegrationConnectionRepository connectionRepository) {
    return new IntegrationSyncLogService(syncLogRepository, connectionRepository);
  }
}
