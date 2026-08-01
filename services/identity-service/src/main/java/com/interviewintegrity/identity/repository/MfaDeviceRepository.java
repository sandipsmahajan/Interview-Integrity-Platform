package com.interviewintegrity.identity.repository;

import com.interviewintegrity.identity.domain.MfaDevice;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link MfaDevice} entities. */
public interface MfaDeviceRepository extends ReactiveCrudRepository<MfaDevice, UUID> {

  /** Lists the live MFA devices of a user. */
  @Query("SELECT * FROM mfa_devices WHERE user_id = :userId AND deleted_at IS NULL")
  Flux<MfaDevice> listLiveByUserId(UUID userId);

  /** Lists the live devices of a user of the given kind. */
  @Query(
      "SELECT * FROM mfa_devices WHERE user_id = :userId AND kind = :kind "
          + "AND deleted_at IS NULL")
  Flux<MfaDevice> listLiveByUserIdAndKind(UUID userId, String kind);

  /** Finds the verified device of the user for the given kind, if any. */
  @Query(
      "SELECT * FROM mfa_devices WHERE user_id = :userId AND kind = :kind "
          + "AND verified_at IS NOT NULL AND deleted_at IS NULL ORDER BY verified_at DESC LIMIT 1")
  Mono<MfaDevice> findLiveVerifiedByUserIdAndKind(UUID userId, String kind);

  /** Finds the latest pending (unverified) device of the user for the given kind. */
  @Query(
      "SELECT * FROM mfa_devices WHERE user_id = :userId AND kind = :kind "
          + "AND verified_at IS NULL AND deleted_at IS NULL ORDER BY created_at DESC LIMIT 1")
  Mono<MfaDevice> findLivePendingByUserIdAndKind(UUID userId, String kind);

  /** Finds a live device of the user by id. */
  @Query("SELECT * FROM mfa_devices WHERE id = :id AND user_id = :userId AND deleted_at IS NULL")
  Mono<MfaDevice> findLiveByIdAndUserId(UUID id, UUID userId);

  /** Removes pending (unverified) devices of the user and kind before re-enrollment. */
  @Query(
      "DELETE FROM mfa_devices WHERE user_id = :userId AND kind = :kind "
          + "AND verified_at IS NULL")
  Mono<Void> deletePendingByUserIdAndKind(UUID userId, String kind);
}
