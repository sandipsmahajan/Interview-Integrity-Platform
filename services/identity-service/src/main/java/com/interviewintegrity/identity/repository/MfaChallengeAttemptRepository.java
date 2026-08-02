package com.interviewintegrity.identity.repository;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * Distributed brute-force guard for MFA login challenges.
 *
 * <p>Tracks failed verification attempts per challenge so a guessed challenge id cannot be probed
 * indefinitely across instances.
 */
public interface MfaChallengeAttemptRepository
    extends ReactiveCrudRepository<MfaChallengeAttempt, String> {

  /** Records a failed verification attempt for a challenge, creating the row if absent. */
  @Modifying
  @Query(
      "INSERT INTO mfa_challenge_attempts (challenge_id, user_id, attempts, last_attempt_at) "
          + "VALUES (:challengeId, :userId, 1, :now) "
          + "ON CONFLICT (challenge_id) DO UPDATE SET attempts = mfa_challenge_attempts.attempts + 1, "
          + "last_attempt_at = :now")
  Mono<Integer> recordAttempt(String challengeId, UUID userId, Instant now);

  /** Returns the current attempt count of a challenge, or 0 when it has none yet. */
  @Query("SELECT attempts FROM mfa_challenge_attempts WHERE challenge_id = :challengeId")
  Mono<Integer> attempts(String challengeId);

  /** Removes the attempt record of a challenge after a successful verification. */
  @Modifying
  @Query("DELETE FROM mfa_challenge_attempts WHERE challenge_id = :challengeId")
  Mono<Integer> deleteByChallengeId(String challengeId);
}
