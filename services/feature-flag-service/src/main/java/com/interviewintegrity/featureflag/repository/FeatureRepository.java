package com.interviewintegrity.featureflag.repository;

import com.interviewintegrity.featureflag.domain.Feature;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link Feature} entities. */
public interface FeatureRepository extends ReactiveCrudRepository<Feature, UUID> {

  /** Finds a live feature by id. */
  @Query("SELECT * FROM features WHERE id = :id AND deleted_at IS NULL")
  Mono<Feature> findLiveById(UUID id);

  /** Finds a live feature of an organization by code. */
  @Query(
      "SELECT * FROM features WHERE organization_id = :organizationId AND code = :code "
          + "AND deleted_at IS NULL LIMIT 1")
  Mono<Feature> findLiveByOrganizationAndCode(UUID organizationId, String code);

  /** Lists the live features of an organization. */
  @Query(
      "SELECT * FROM features WHERE organization_id = :organizationId AND deleted_at IS NULL "
          + "ORDER BY code")
  Flux<Feature> listLiveByOrganization(UUID organizationId);

  /** Resolves whether a feature code already exists within an organization. */
  @Query(
      "SELECT EXISTS(SELECT 1 FROM features WHERE organization_id = :organizationId "
          + "AND code = :code AND deleted_at IS NULL)")
  Mono<Boolean> existsByOrganizationAndCode(UUID organizationId, String code);
}
