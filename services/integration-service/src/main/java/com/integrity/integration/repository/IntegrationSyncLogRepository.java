package com.integrity.integration.repository;

import com.integrity.integration.domain.IntegrationSyncLog;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link IntegrationSyncLog} entities. */
public interface IntegrationSyncLogRepository
    extends ReactiveCrudRepository<IntegrationSyncLog, Long> {

  /** Finds a sync run by id within an organization. */
  @Query(
      "SELECT * FROM integration_sync_logs WHERE id = :id "
          + "AND organization_id = :organizationId")
  Mono<IntegrationSyncLog> findByIdAndOrganization(Long id, UUID organizationId);

  /** Lists the sync runs of a connection, newest first. */
  @Query(
      "SELECT * FROM integration_sync_logs WHERE connection_id = :connectionId "
          + "ORDER BY started_at DESC")
  Flux<IntegrationSyncLog> listByConnection(UUID connectionId);

  /** Lists the sync runs of an organization, newest first. */
  @Query(
      "SELECT * FROM integration_sync_logs WHERE organization_id = :organizationId "
          + "ORDER BY started_at DESC")
  Flux<IntegrationSyncLog> listByOrganization(UUID organizationId);
}
