package com.integrity.integration.repository;

import com.integrity.integration.domain.Integration;
import com.integrity.integration.domain.IntegrationStatus;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link Integration} entities. */
public interface IntegrationRepository extends ReactiveCrudRepository<Integration, UUID> {

  /** Finds a live integration by id within an organization. */
  @Query(
      "SELECT * FROM integrations WHERE id = :id "
          + "AND organization_id = :organizationId AND deleted_at IS NULL")
  Mono<Integration> findLiveByIdAndOrganization(UUID id, UUID organizationId);

  /** Finds a live integration of an organization for a provider. */
  @Query(
      "SELECT * FROM integrations WHERE organization_id = :organizationId "
          + "AND provider = :provider AND deleted_at IS NULL")
  Mono<Integration> findLiveByOrganizationAndProvider(UUID organizationId, String provider);

  /** Lists the live integrations of an organization, optionally filtered by status. */
  @Query(
      "SELECT * FROM integrations WHERE organization_id = :organizationId "
          + "AND deleted_at IS NULL ORDER BY name")
  Flux<Integration> listLiveByOrganization(UUID organizationId);

  /** Lists the live integrations of an organization in the given status. */
  @Query(
      "SELECT * FROM integrations WHERE organization_id = :organizationId "
          + "AND status = :status AND deleted_at IS NULL ORDER BY name")
  Flux<Integration> listLiveByOrganizationAndStatus(UUID organizationId, IntegrationStatus status);
}
