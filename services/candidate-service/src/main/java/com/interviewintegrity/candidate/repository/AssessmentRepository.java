package com.interviewintegrity.candidate.repository;

import com.interviewintegrity.candidate.domain.Assessment;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/** Reactive repository for {@link Assessment} entities. */
public interface AssessmentRepository extends ReactiveCrudRepository<Assessment, UUID> {

  /** Lists the assessments of a candidate, most recently assigned first. */
  @Query(
      "SELECT * FROM assessments WHERE organization_id = :organizationId "
          + "AND candidate_id = :candidateId ORDER BY assigned_at DESC")
  Flux<Assessment> listByOrganizationAndCandidate(UUID organizationId, UUID candidateId);
}
