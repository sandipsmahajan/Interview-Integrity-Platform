package com.interviewintegrity.identity.repository;

import com.interviewintegrity.identity.domain.Permission;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link Permission} entities. */
public interface PermissionRepository extends ReactiveCrudRepository<Permission, UUID> {

  /** Finds a permission by its code. */
  Mono<Permission> findByCode(String code);

  /** Lists all permissions ordered by code. */
  @Query("SELECT * FROM permissions ORDER BY code")
  Flux<Permission> findAllOrdered();

  /** Resolves the codes of the permissions assigned to a role. */
  @Query(
      "SELECT p.code FROM permissions p JOIN role_permissions rp ON rp.permission_id = p.id "
          + "WHERE rp.role_id = :roleId ORDER BY p.code")
  Flux<String> findCodesByRole(UUID roleId);

  /** Resolves the ids of the permissions assigned to a role. */
  @Query(
      "SELECT p.id FROM permissions p JOIN role_permissions rp ON rp.permission_id = p.id "
          + "WHERE rp.role_id = :roleId")
  Flux<UUID> findIdsByRole(UUID roleId);
}
