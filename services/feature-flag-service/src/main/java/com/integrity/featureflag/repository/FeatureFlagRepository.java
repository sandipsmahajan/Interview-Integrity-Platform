package com.integrity.featureflag.repository;

import com.integrity.featureflag.domain.FeatureFlag;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link FeatureFlag} entities. */
public interface FeatureFlagRepository extends ReactiveCrudRepository<FeatureFlag, UUID> {

  /** Finds a flag by id within an organization. */
  @Query("SELECT * FROM feature_flags WHERE id = :id AND organization_id = :organizationId")
  Mono<FeatureFlag> findByIdAndOrganization(UUID id, UUID organizationId);

  /** Lists the flags of a feature. */
  @Query(
      "SELECT * FROM feature_flags WHERE organization_id = :organizationId "
          + "AND feature_id = :featureId ORDER BY environment")
  Flux<FeatureFlag> listByOrganizationAndFeature(UUID organizationId, UUID featureId);

  /** Lists the flags of an organization, optionally in an environment. */
  @Query(
      "SELECT * FROM feature_flags WHERE organization_id = :organizationId "
          + "AND environment = :environment ORDER BY feature_id")
  Flux<FeatureFlag> listByOrganizationAndEnvironment(UUID organizationId, String environment);

  /** Finds the flag of a feature in an environment. */
  @Query(
      "SELECT * FROM feature_flags WHERE feature_id = :featureId "
          + "AND environment = :environment LIMIT 1")
  Mono<FeatureFlag> findByFeatureIdAndEnvironment(UUID featureId, String environment);

  /** Resolves whether a flag already exists for a feature in an environment. */
  @Query(
      "SELECT EXISTS(SELECT 1 FROM feature_flags WHERE feature_id = :featureId "
          + "AND environment = :environment)")
  Mono<Boolean> existsByFeatureIdAndEnvironment(UUID featureId, String environment);
}
