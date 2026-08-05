package com.integrity.identity.repository;

import com.integrity.identity.domain.PasswordHistory;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/** Reactive repository for {@link PasswordHistory} entities. */
public interface PasswordHistoryRepository extends ReactiveCrudRepository<PasswordHistory, UUID> {

  /** Returns the most recent password history entries of a user. */
  Flux<PasswordHistory> findTop10ByUserIdOrderByChangedAtDesc(UUID userId);
}
