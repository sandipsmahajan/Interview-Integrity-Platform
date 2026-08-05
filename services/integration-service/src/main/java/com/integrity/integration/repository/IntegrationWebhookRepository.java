package com.integrity.integration.repository;

import com.integrity.integration.domain.IntegrationWebhook;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link IntegrationWebhook} entities. */
public interface IntegrationWebhookRepository
    extends ReactiveCrudRepository<IntegrationWebhook, UUID> {

  /** Finds a webhook by id within an organization. */
  @Query(
      "SELECT * FROM integration_webhooks WHERE id = :id "
          + "AND organization_id = :organizationId")
  Mono<IntegrationWebhook> findByIdAndOrganization(UUID id, UUID organizationId);

  /** Lists the webhooks of an integration. */
  @Query(
      "SELECT * FROM integration_webhooks WHERE integration_id = :integrationId "
          + "AND organization_id = :organizationId ORDER BY url")
  Flux<IntegrationWebhook> listByIntegration(UUID integrationId, UUID organizationId);

  /** Lists the webhooks of an organization. */
  @Query(
      "SELECT * FROM integration_webhooks WHERE organization_id = :organizationId "
          + "ORDER BY url")
  Flux<IntegrationWebhook> listByOrganization(UUID organizationId);
}
