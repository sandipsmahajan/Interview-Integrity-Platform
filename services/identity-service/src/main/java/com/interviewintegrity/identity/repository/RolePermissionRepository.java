package com.interviewintegrity.identity.repository;

import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Database client backed repository for the {@code role_permissions} bridge table.
 *
 * <p>The bridge has a composite primary key, which Spring Data R2DBC entities cannot map directly,
 * so explicit SQL is used for all operations.
 */
public final class RolePermissionRepository {

  private static final String ROLE_ID = "roleId";
  private static final String PERMISSION_ID = "permissionId";
  private static final String GRANTED_BY = "grantedBy";

  private final DatabaseClient databaseClient;

  /** Creates a repository bound to the given database client. */
  public RolePermissionRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  /** Returns the permission ids granted to a role. */
  public Flux<UUID> findPermissionIdsOfRole(UUID roleId) {
    return databaseClient
        .sql("SELECT permission_id FROM role_permissions WHERE role_id = :roleId")
        .bind(ROLE_ID, roleId)
        .map((row, metadata) -> row.get("permission_id", UUID.class))
        .all();
  }

  /** Returns true when the role already holds the permission. */
  public Mono<Boolean> exists(UUID roleId, UUID permissionId) {
    return databaseClient
        .sql(
            "SELECT count(*) FROM role_permissions "
                + "WHERE role_id = :roleId AND permission_id = :permissionId")
        .bind(ROLE_ID, roleId)
        .bind(PERMISSION_ID, permissionId)
        .map((row, metadata) -> row.get(0, Long.class))
        .one()
        .map(count -> count > 0);
  }

  /** Grants a permission to a role, ignoring a duplicate grant. */
  public Mono<Void> grant(UUID roleId, UUID permissionId, UUID grantedBy) {
    return databaseClient
        .sql(
            "INSERT INTO role_permissions (role_id, permission_id, granted_by, granted_at) "
                + "VALUES (:roleId, :permissionId, :grantedBy, now()) ON CONFLICT DO NOTHING")
        .bind(ROLE_ID, roleId)
        .bind(PERMISSION_ID, permissionId)
        .bind(GRANTED_BY, grantedBy)
        .then();
  }

  /** Revokes a permission from a role. */
  public Mono<Void> revoke(UUID roleId, UUID permissionId) {
    return databaseClient
        .sql(
            "DELETE FROM role_permissions WHERE role_id = :roleId AND permission_id = :permissionId")
        .bind(ROLE_ID, roleId)
        .bind(PERMISSION_ID, permissionId)
        .then();
  }
}
