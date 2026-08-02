package com.interviewintegrity.identity.repository;

import com.interviewintegrity.identity.domain.OtpCode;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link OtpCode} entities. */
public interface OtpCodeRepository extends ReactiveCrudRepository<OtpCode, UUID> {

  /** Finds the most recently requested outstanding code for the user and purpose. */
  @Query(
      "SELECT * FROM otp_codes WHERE user_id = :userId AND purpose = :purpose "
          + "AND consumed_at IS NULL ORDER BY requested_at DESC LIMIT 1")
  Mono<OtpCode> findOutstanding(UUID userId, String purpose);

  /** Counts codes requested by the user for a purpose within the rate window. */
  @Query(
      "SELECT count(*) FROM otp_codes WHERE user_id = :userId AND purpose = :purpose "
          + "AND requested_at >= :since")
  Mono<Long> countRequestedSince(UUID userId, String purpose, Instant since);

  /** Finds an outstanding code by user, purpose and hash, used for verification. */
  @Query(
      "SELECT * FROM otp_codes WHERE user_id = :userId AND purpose = :purpose "
          + "AND code_hash = :codeHash AND consumed_at IS NULL ORDER BY requested_at DESC LIMIT 1")
  Mono<OtpCode> findOutstandingByHash(UUID userId, String purpose, String codeHash);

  /**
   * Atomically consumes an outstanding code, returning the number of rows affected.
   *
   * <p>The guarded WHERE clause prevents a code that was already consumed, has expired or exceeded
   * its attempt budget from being used twice, even under concurrency.
   */
  @Modifying
  @Query(
      "UPDATE otp_codes SET consumed_at = :consumedAt "
          + "WHERE id = :id AND consumed_at IS NULL AND expires_at > :consumedAt "
          + "AND attempts < max_attempts")
  Mono<Integer> consumeIfOutstanding(UUID id, Instant consumedAt);

  /**
   * Atomically increments the attempt counter of an outstanding code, returning affected rows.
   *
   * <p>Used to enforce the per-code attempt budget without a read-modify-write race.
   */
  @Modifying
  @Query("UPDATE otp_codes SET attempts = attempts + 1 " + "WHERE id = :id AND consumed_at IS NULL")
  Mono<Integer> recordAttempt(UUID id);
}
