package com.interviewintegrity.identity.repository;

import com.interviewintegrity.identity.domain.Role;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Database client backed repository for the {@code user_roles} bridge table.
 *
 * <p>The bridge has a composite primary key, which Spring Data R2DBC entities cannot map directly,
 * so explicit SQL is used for all operations.
 */
public final class UserRoleRepository {

  private static final String USER_ID = "userId";
  private static final String ROLE_ID = "roleId";
  private static final String ASSIGNED_BY = "assignedBy";

  private final DatabaseClient databaseClient;

  /** Creates a repository bound to the given database client. */
  public UserRoleRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  /** Returns the live roles assigned to a user. */
  public Flux<Role> findRolesOfUser(UUID userId) {
    return databaseClient
        .sql(
            "SELECT r.* FROM roles r JOIN user_roles ur ON ur.role_id = r.id "
                + "WHERE ur.user_id = :userId AND r.deleted_at IS NULL ORDER BY r.code")
        .bind(USER_ID, userId)
        .mapProperties(Role.class)
        .all();
  }

  /** Returns the role ids assigned to a user. */
  public Flux<UUID> findRoleIdsOfUser(UUID userId) {
    return databaseClient
        .sql("SELECT role_id FROM user_roles WHERE user_id = :userId")
        .bind(USER_ID, userId)
        .map((row, metadata) -> row.get("role_id", UUID.class))
        .all();
  }

  /** Returns true when the user already holds the role. */
  public Mono<Boolean> exists(UUID userId, UUID roleId) {
    return databaseClient
        .sql("SELECT count(*) FROM user_roles WHERE user_id = :userId AND role_id = :roleId")
        .bind(USER_ID, userId)
        .bind(ROLE_ID, roleId)
        .map((row, metadata) -> row.get(0, Long.class))
        .one()
        .map(count -> count > 0);
  }

  /** Assigns a role to a user, ignoring a duplicate assignment. */
  public Mono<Void> assign(UUID userId, UUID roleId, UUID assignedBy) {
    return databaseClient
        .sql(
            "INSERT INTO user_roles (user_id, role_id, assigned_by, assigned_at) "
                + "VALUES (:userId, :roleId, :assignedBy, now()) ON CONFLICT DO NOTHING")
        .bind(USER_ID, userId)
        .bind(ROLE_ID, roleId)
        .bind(ASSIGNED_BY, assignedBy)
        .then();
  }

  /** Removes a role from a user. */
  public Mono<Void> remove(UUID userId, UUID roleId) {
    return databaseClient
        .sql("DELETE FROM user_roles WHERE user_id = :userId AND role_id = :roleId")
        .bind(USER_ID, userId)
        .bind(ROLE_ID, roleId)
        .then();
  }
}
