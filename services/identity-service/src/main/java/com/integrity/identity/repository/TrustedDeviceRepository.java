package com.integrity.identity.repository;

import com.integrity.identity.domain.TrustedDevice;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link TrustedDevice} entities. */
public interface TrustedDeviceRepository extends ReactiveCrudRepository<TrustedDevice, UUID> {

  /** Lists the trusted devices of a user, most recently used first. */
  @Query("SELECT * FROM trusted_devices WHERE user_id = :userId ORDER BY last_seen_at DESC")
  Flux<TrustedDevice> listByUser(UUID userId);

  /** Finds a trusted device of the user by device id. */
  @Query("SELECT * FROM trusted_devices WHERE user_id = :userId AND device_id = :deviceId LIMIT 1")
  Mono<TrustedDevice> findByUserAndDeviceId(UUID userId, String deviceId);

  /** Deletes a trusted device of the user. */
  @Query("DELETE FROM trusted_devices WHERE id = :id AND user_id = :userId")
  Mono<Long> deleteByIdAndUser(UUID id, UUID userId);
}
