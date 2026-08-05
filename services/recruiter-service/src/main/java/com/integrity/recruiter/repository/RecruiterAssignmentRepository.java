package com.integrity.recruiter.repository;

import com.integrity.recruiter.domain.RecruiterAssignment;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link RecruiterAssignment} entities. */
public interface RecruiterAssignmentRepository
    extends ReactiveCrudRepository<RecruiterAssignment, UUID> {

  /** Finds an active assignment by id. */
  @Query("SELECT * FROM recruiter_assignments WHERE id = :id AND ended_at IS NULL")
  Mono<RecruiterAssignment> findActiveById(UUID id);

  /** Lists the assignments of a candidate. */
  @Query(
      "SELECT * FROM recruiter_assignments WHERE organization_id = :organizationId "
          + "AND candidate_id = :candidateId ORDER BY assigned_at DESC")
  Flux<RecruiterAssignment> listByOrganizationAndCandidate(UUID organizationId, UUID candidateId);

  /** Finds the active assignment of a candidate to a recruiter. */
  @Query(
      "SELECT * FROM recruiter_assignments WHERE organization_id = :organizationId "
          + "AND candidate_id = :candidateId AND recruiter_id = :recruiterId AND ended_at IS NULL "
          + "LIMIT 1")
  Mono<RecruiterAssignment> findActive(UUID organizationId, UUID candidateId, UUID recruiterId);
}
