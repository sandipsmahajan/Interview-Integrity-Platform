package com.integrity.recruiter.repository;

import com.integrity.recruiter.domain.PipelineStage;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link PipelineStage} entities. */
public interface PipelineStageRepository extends ReactiveCrudRepository<PipelineStage, UUID> {

  /** Finds a live stage by id. */
  @Query("SELECT * FROM pipeline_stages WHERE id = :id AND deleted_at IS NULL")
  Mono<PipelineStage> findLiveById(UUID id);

  /** Lists the live stages of an organization ordered by position. */
  @Query(
      "SELECT * FROM pipeline_stages WHERE organization_id = :organizationId "
          + "AND deleted_at IS NULL ORDER BY order_index")
  Flux<PipelineStage> listLiveByOrganization(UUID organizationId);

  /** Resolves whether a stage code already exists within an organization. */
  @Query(
      "SELECT EXISTS(SELECT 1 FROM pipeline_stages WHERE organization_id = :organizationId "
          + "AND code = :code AND deleted_at IS NULL)")
  Mono<Boolean> existsByOrganizationAndCode(UUID organizationId, String code);
}
