package com.integrity.featureflag.repository;

import com.integrity.featureflag.domain.FlagTarget;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Database client backed repository for the {@code flag_targets} bridge table.
 *
 * <p>The bridge has a composite primary key, which Spring Data R2DBC entities cannot map directly,
 * so explicit SQL is used for all operations.
 */
public final class FlagTargetRepository {

  private static final String FLAG_ID = "flagId";
  private static final String USER_ID = "userId";
  private static final String VARIANT = "variant";
  private static final String ENABLED = "enabled";
  private static final String ADDED_BY = "addedBy";

  private final DatabaseClient databaseClient;

  /** Creates a repository bound to the given database client. */
  public FlagTargetRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  /** Adds or replaces a per-user flag override. */
  public Mono<Void> upsert(
      UUID flagId, UUID userId, String variant, boolean enabled, UUID addedBy) {
    return databaseClient
        .sql(
            "INSERT INTO flag_targets (flag_id, user_id, variant, enabled, added_by, added_at) "
                + "VALUES (:flagId, :userId, :variant, :enabled, :addedBy, now()) "
                + "ON CONFLICT (flag_id, user_id) "
                + "DO UPDATE SET variant = :variant, enabled = :enabled, added_by = :addedBy")
        .bind(FLAG_ID, flagId)
        .bind(USER_ID, userId)
        .bind(VARIANT, variant)
        .bind(ENABLED, enabled)
        .bind(ADDED_BY, addedBy)
        .then();
  }

  /** Removes a per-user flag override. */
  public Mono<Void> remove(UUID flagId, UUID userId) {
    return databaseClient
        .sql("DELETE FROM flag_targets WHERE flag_id = :flagId AND user_id = :userId")
        .bind(FLAG_ID, flagId)
        .bind(USER_ID, userId)
        .then();
  }

  /** Returns true when the user already has an override for the flag. */
  public Mono<Boolean> exists(UUID flagId, UUID userId) {
    return databaseClient
        .sql("SELECT count(*) FROM flag_targets WHERE flag_id = :flagId AND user_id = :userId")
        .bind(FLAG_ID, flagId)
        .bind(USER_ID, userId)
        .map((row, metadata) -> row.get(0, Long.class))
        .one()
        .map(count -> count > 0);
  }

  /** Lists the per-user overrides of a flag, newest first. */
  public Flux<FlagTarget> listByFlag(UUID flagId) {
    return databaseClient
        .sql("SELECT * FROM flag_targets WHERE flag_id = :flagId ORDER BY added_at DESC")
        .bind(FLAG_ID, flagId)
        .mapProperties(FlagTarget.class)
        .all();
  }
}
