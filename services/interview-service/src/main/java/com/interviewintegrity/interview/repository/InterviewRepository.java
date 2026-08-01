package com.interviewintegrity.interview.repository;

import com.interviewintegrity.interview.domain.Interview;
import com.interviewintegrity.interview.domain.InterviewStatus;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link Interview} entities. */
public interface InterviewRepository extends ReactiveCrudRepository<Interview, UUID> {

  /** Finds a live interview by id. */
  @Query("SELECT * FROM interviews WHERE id = :id AND deleted_at IS NULL")
  Mono<Interview> findLiveById(UUID id);

  /** Lists the live interviews of an organization, newest first. */
  @Query(
      "SELECT * FROM interviews WHERE organization_id = :organizationId AND deleted_at IS NULL "
          + "ORDER BY starts_at DESC")
  Flux<Interview> listLiveByOrganization(UUID organizationId);

  /** Lists the live interviews of an organization in the given status. */
  @Query(
      "SELECT * FROM interviews WHERE organization_id = :organizationId AND status = :status "
          + "AND deleted_at IS NULL ORDER BY starts_at DESC")
  Flux<Interview> listLiveByOrganizationAndStatus(UUID organizationId, InterviewStatus status);

  /** Lists the live interviews of an organization for a candidate. */
  @Query(
      "SELECT * FROM interviews WHERE organization_id = :organizationId "
          + "AND candidate_id = :candidateId AND deleted_at IS NULL ORDER BY starts_at DESC")
  Flux<Interview> listLiveByOrganizationAndCandidate(UUID organizationId, UUID candidateId);

  /** Lists the live interviews of an organization for a recruiter. */
  @Query(
      "SELECT * FROM interviews WHERE organization_id = :organizationId "
          + "AND recruiter_id = :recruiterId AND deleted_at IS NULL ORDER BY starts_at DESC")
  Flux<Interview> listLiveByOrganizationAndRecruiter(UUID organizationId, UUID recruiterId);
}
