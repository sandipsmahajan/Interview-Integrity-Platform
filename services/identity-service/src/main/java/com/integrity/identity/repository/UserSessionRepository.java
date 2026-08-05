package com.integrity.identity.repository;

import com.integrity.identity.domain.UserSession;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link UserSession} entities. */
public interface UserSessionRepository extends ReactiveCrudRepository<UserSession, UUID> {

  /** Finds a session by the SHA-256 hash of its refresh token. */
  Mono<UserSession> findByRefreshTokenHash(String refreshTokenHash);

  /** Lists the live sessions of a user, newest first. */
  @Query(
      "SELECT * FROM user_sessions WHERE user_id = :userId ORDER BY issued_at DESC LIMIT :limit "
          + "OFFSET :offset")
  Flux<UserSession> listByUser(UUID userId, int limit, long offset);

  /** Counts the sessions of a user. */
  @Query("SELECT count(*) FROM user_sessions WHERE user_id = :userId")
  Mono<Long> countByUser(UUID userId);

  /** Marks all live sessions of a user as revoked. */
  @Modifying
  @Query(
      "UPDATE user_sessions SET status = 'REVOKED', revoked_at = :revokedAt, "
          + "updated_at = now() WHERE user_id = :userId AND status = 'ACTIVE'")
  Mono<Integer> revokeAllActiveByUser(UUID userId, Instant revokedAt);

  /** Marks all sessions that expired before the given instant as expired. */
  @Modifying
  @Query(
      "UPDATE user_sessions SET status = 'EXPIRED', updated_at = now() "
          + "WHERE status = 'ACTIVE' AND expires_at < :now")
  Mono<Integer> expirePast(Instant now);
}
