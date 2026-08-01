package com.interviewintegrity.integration.repository;

import com.interviewintegrity.integration.domain.IntegrationConnection;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link IntegrationConnection} entities. */
public interface IntegrationConnectionRepository
    extends ReactiveCrudRepository<IntegrationConnection, UUID> {

  /** Finds a connection by id within an organization. */
  @Query(
      "SELECT * FROM integration_connections WHERE id = :id "
          + "AND organization_id = :organizationId")
  Mono<IntegrationConnection> findByIdAndOrganization(UUID id, UUID organizationId);

  /** Finds a connection by external account id under an integration. */
  @Query(
      "SELECT * FROM integration_connections WHERE integration_id = :integrationId "
          + "AND external_account_id = :externalAccountId")
  Mono<IntegrationConnection> findByIntegrationAndExternalAccount(
      UUID integrationId, String externalAccountId);

  /** Lists the connections of an integration. */
  @Query(
      "SELECT * FROM integration_connections WHERE integration_id = :integrationId "
          + "AND organization_id = :organizationId ORDER BY external_account_id")
  Flux<IntegrationConnection> listByIntegration(UUID integrationId, UUID organizationId);

  /** Lists the connections of an organization. */
  @Query(
      "SELECT * FROM integration_connections WHERE organization_id = :organizationId "
          + "ORDER BY external_account_id")
  Flux<IntegrationConnection> listByOrganization(UUID organizationId);
}
