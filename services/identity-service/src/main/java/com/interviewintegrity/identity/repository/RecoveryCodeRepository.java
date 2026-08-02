package com.interviewintegrity.identity.repository;

import com.interviewintegrity.identity.domain.RecoveryCode;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link RecoveryCode} entities. */
public interface RecoveryCodeRepository extends ReactiveCrudRepository<RecoveryCode, UUID> {

  /** Lists the remaining usable recovery codes of a user. */
  @Query(
      "SELECT * FROM recovery_codes WHERE user_id = :userId AND consumed_at IS NULL "
          + "ORDER BY created_at")
  Flux<RecoveryCode> listUsableByUser(UUID userId);

  /** Finds an unused recovery code of the user matching the given hash. */
  @Query(
      "SELECT * FROM recovery_codes WHERE user_id = :userId AND code_hash = :codeHash "
          + "AND consumed_at IS NULL ORDER BY created_at LIMIT 1")
  Mono<RecoveryCode> findUsableByHash(UUID userId, String codeHash);

  /** Deletes all recovery codes of a user (used when regenerating a set). */
  @Query("DELETE FROM recovery_codes WHERE user_id = :userId")
  Mono<Void> deleteAllByUser(UUID userId);

  /**
   * Atomically consumes an unused recovery code, returning the number of rows affected.
   *
   * <p>The guarded WHERE clause makes the single-use guarantee hold even when the same code is
   * submitted concurrently.
   */
  @Modifying
  @Query(
      "UPDATE recovery_codes SET consumed_at = :consumedAt "
          + "WHERE id = :id AND consumed_at IS NULL")
  Mono<Integer> consumeIfUnused(UUID id, Instant consumedAt);
}
