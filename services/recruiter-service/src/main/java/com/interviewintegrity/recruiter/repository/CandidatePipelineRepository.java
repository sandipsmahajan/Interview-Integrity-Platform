package com.interviewintegrity.recruiter.repository;

import com.interviewintegrity.recruiter.domain.CandidatePipeline;
import com.interviewintegrity.recruiter.domain.PipelineStatus;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link CandidatePipeline} entities. */
public interface CandidatePipelineRepository
    extends ReactiveCrudRepository<CandidatePipeline, UUID> {

  /** Finds a pipeline entry by id. */
  @Override
  Mono<CandidatePipeline> findById(UUID id);

  /** Lists the pipeline entries of a candidate ordered by entry time. */
  @Query(
      "SELECT * FROM candidate_pipeline WHERE organization_id = :organizationId "
          + "AND candidate_id = :candidateId ORDER BY entered_at DESC")
  Flux<CandidatePipeline> listByOrganizationAndCandidate(UUID organizationId, UUID candidateId);

  /** Lists the current pipeline entries within a stage. */
  @Query(
      "SELECT * FROM candidate_pipeline WHERE organization_id = :organizationId "
          + "AND stage_id = :stageId AND status = 'CURRENT' ORDER BY position, entered_at")
  Flux<CandidatePipeline> listCurrentByStage(UUID organizationId, UUID stageId);

  /** Finds the current entry of a candidate within a stage. */
  @Query(
      "SELECT * FROM candidate_pipeline WHERE organization_id = :organizationId "
          + "AND candidate_id = :candidateId AND stage_id = :stageId AND status = :status "
          + "LIMIT 1")
  Mono<CandidatePipeline> findCurrentByStage(
      UUID organizationId, UUID candidateId, UUID stageId, PipelineStatus status);
}
