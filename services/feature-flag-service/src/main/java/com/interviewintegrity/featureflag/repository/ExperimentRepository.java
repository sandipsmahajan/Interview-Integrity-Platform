package com.interviewintegrity.featureflag.repository;

import com.interviewintegrity.featureflag.domain.Experiment;
import com.interviewintegrity.featureflag.domain.ExperimentStatus;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link Experiment} entities. */
public interface ExperimentRepository extends ReactiveCrudRepository<Experiment, UUID> {

  /** Finds an experiment by id within an organization. */
  @Query("SELECT * FROM experiments WHERE id = :id AND organization_id = :organizationId")
  Mono<Experiment> findByIdAndOrganization(UUID id, UUID organizationId);

  /** Lists the experiments of an organization. */
  @Query(
      "SELECT * FROM experiments WHERE organization_id = :organizationId "
          + "ORDER BY created_at DESC")
  Flux<Experiment> listByOrganization(UUID organizationId);

  /** Lists the experiments of an organization in a status. */
  @Query(
      "SELECT * FROM experiments WHERE organization_id = :organizationId "
          + "AND status = :status ORDER BY created_at DESC")
  Flux<Experiment> listByOrganizationAndStatus(UUID organizationId, ExperimentStatus status);

  /** Lists the experiments targeting a feature. */
  @Query("SELECT * FROM experiments WHERE feature_id = :featureId ORDER BY created_at DESC")
  Flux<Experiment> listByFeature(UUID featureId);
}
