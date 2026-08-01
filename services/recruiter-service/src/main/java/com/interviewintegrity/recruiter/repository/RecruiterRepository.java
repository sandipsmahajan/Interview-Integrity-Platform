package com.interviewintegrity.recruiter.repository;

import com.interviewintegrity.recruiter.domain.Recruiter;
import com.interviewintegrity.recruiter.domain.RecruiterStatus;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link Recruiter} entities. */
public interface RecruiterRepository extends ReactiveCrudRepository<Recruiter, UUID> {

  /** Finds a live recruiter by id. */
  @Query("SELECT * FROM recruiters WHERE id = :id AND deleted_at IS NULL")
  Mono<Recruiter> findLiveById(UUID id);

  /** Finds a live recruiter by organization and linked user. */
  @Query(
      "SELECT * FROM recruiters WHERE organization_id = :organizationId AND user_id = :userId "
          + "AND deleted_at IS NULL")
  Mono<Recruiter> findLiveByOrganizationAndUser(UUID organizationId, UUID userId);

  /** Lists the live recruiters of an organization. */
  @Query(
      "SELECT * FROM recruiters WHERE organization_id = :organizationId AND deleted_at IS NULL "
          + "ORDER BY full_name")
  Flux<Recruiter> listLiveByOrganization(UUID organizationId);

  /** Lists the live recruiters of an organization in the given status. */
  @Query(
      "SELECT * FROM recruiters WHERE organization_id = :organizationId AND status = :status "
          + "AND deleted_at IS NULL ORDER BY full_name")
  Flux<Recruiter> listLiveByOrganizationAndStatus(UUID organizationId, RecruiterStatus status);

  /** Counts the live recruiters of an organization. */
  @Query(
      "SELECT count(*) FROM recruiters WHERE organization_id = :organizationId "
          + "AND deleted_at IS NULL")
  Mono<Long> countLiveByOrganization(UUID organizationId);
}
