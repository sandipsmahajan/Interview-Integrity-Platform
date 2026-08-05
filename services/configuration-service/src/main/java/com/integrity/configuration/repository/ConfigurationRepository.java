package com.integrity.configuration.repository;

import com.integrity.configuration.domain.ConfigScope;
import com.integrity.configuration.domain.Configuration;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link Configuration} entities. */
public interface ConfigurationRepository extends ReactiveCrudRepository<Configuration, UUID> {

  /** Finds a live configuration by id. */
  @Query("SELECT * FROM configurations WHERE id = :id AND deleted_at IS NULL")
  Mono<Configuration> findLiveById(UUID id);

  /** Finds a live configuration of an organization for a scope and key. */
  @Query(
      "SELECT * FROM configurations WHERE organization_id = :organizationId "
          + "AND scope = :scope AND key = :key AND deleted_at IS NULL LIMIT 1")
  Mono<Configuration> findLiveByOrganizationScopeAndKey(
      UUID organizationId, ConfigScope scope, String key);

  /** Lists the live configurations visible to an organization including SYSTEM defaults. */
  @Query(
      "SELECT * FROM configurations WHERE (scope = 'SYSTEM' OR organization_id = :organizationId) "
          + "AND deleted_at IS NULL ORDER BY key")
  Flux<Configuration> listLiveVisible(UUID organizationId);

  /** Lists the live configurations of an organization in a scope. */
  @Query(
      "SELECT * FROM configurations WHERE organization_id = :organizationId "
          + "AND scope = :scope AND deleted_at IS NULL ORDER BY key")
  Flux<Configuration> listLiveByScope(UUID organizationId, ConfigScope scope);

  /** Resolves whether a live configuration already exists for the scope and key. */
  @Query(
      "SELECT EXISTS(SELECT 1 FROM configurations WHERE organization_id = :organizationId "
          + "AND scope = :scope AND key = :key AND deleted_at IS NULL)")
  Mono<Boolean> existsByOrganizationScopeAndKey(UUID organizationId, ConfigScope scope, String key);

  /** Counts the live configurations of an organization. */
  @Query(
      "SELECT count(*) FROM configurations WHERE organization_id = :organizationId "
          + "AND deleted_at IS NULL")
  Mono<Long> countLiveByOrganization(UUID organizationId);
}
