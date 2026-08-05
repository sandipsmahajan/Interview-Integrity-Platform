package com.integrity.featureflag.repository;

import com.integrity.featureflag.domain.FeatureFlagHistory;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/** Reactive repository for {@link FeatureFlagHistory} entities. */
public interface FeatureFlagHistoryRepository
    extends ReactiveCrudRepository<FeatureFlagHistory, Long> {

  /** Lists the history snapshots of a flag, newest first. */
  @Query(
      "SELECT * FROM feature_flags_history WHERE organization_id = :organizationId "
          + "AND id = :flagId ORDER BY changed_at DESC")
  Flux<FeatureFlagHistory> listByOrganizationAndFlag(UUID organizationId, UUID flagId);
}
