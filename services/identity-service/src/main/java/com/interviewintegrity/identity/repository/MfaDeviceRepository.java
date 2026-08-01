package com.interviewintegrity.identity.repository;

import com.interviewintegrity.identity.domain.MfaDevice;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/** Reactive repository for {@link MfaDevice} entities. */
public interface MfaDeviceRepository extends ReactiveCrudRepository<MfaDevice, UUID> {

  /** Lists the live MFA devices of a user. */
  Flux<MfaDevice> findLiveByUserId(UUID userId);
}
