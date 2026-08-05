package com.integrity.identity.repository;

import com.integrity.identity.domain.Role;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link Role} entities. */
public interface RoleRepository extends ReactiveCrudRepository<Role, UUID> {

  /** Finds a live role by organization and code. */
  @Query(
      "SELECT * FROM roles WHERE organization_id = :organizationId AND code = :code "
          + "AND deleted_at IS NULL")
  Mono<Role> findLiveByOrganizationAndCode(UUID organizationId, String code);

  /** Lists live roles of an organization. */
  @Query(
      "SELECT * FROM roles WHERE organization_id = :organizationId AND deleted_at IS NULL "
          + "ORDER BY name")
  Flux<Role> listLiveByOrganization(UUID organizationId);

  /** Finds a live role by id. */
  @Query("SELECT * FROM roles WHERE id = :id AND deleted_at IS NULL")
  Mono<Role> findLiveById(UUID id);
}
