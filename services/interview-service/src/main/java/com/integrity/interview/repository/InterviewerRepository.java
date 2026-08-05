package com.integrity.interview.repository;

import com.integrity.interview.domain.Interviewer;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link Interviewer} entities. */
public interface InterviewerRepository extends ReactiveCrudRepository<Interviewer, UUID> {

  /** Finds a live interviewer by id. */
  @Query("SELECT * FROM interviewers WHERE id = :id AND deleted_at IS NULL")
  Mono<Interviewer> findLiveById(UUID id);

  /** Finds a live interviewer by organization and linked user. */
  @Query(
      "SELECT * FROM interviewers WHERE organization_id = :organizationId "
          + "AND user_id = :userId AND deleted_at IS NULL")
  Mono<Interviewer> findLiveByOrganizationAndUser(UUID organizationId, UUID userId);

  /** Lists the live interviewers of an organization. */
  @Query(
      "SELECT * FROM interviewers WHERE organization_id = :organizationId "
          + "AND deleted_at IS NULL ORDER BY full_name")
  Flux<Interviewer> listLiveByOrganization(UUID organizationId);
}
