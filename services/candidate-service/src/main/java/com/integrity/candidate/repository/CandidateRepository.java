package com.integrity.candidate.repository;

import com.integrity.candidate.domain.Candidate;
import com.integrity.candidate.domain.CandidateStatus;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link Candidate} entities. */
public interface CandidateRepository extends ReactiveCrudRepository<Candidate, UUID> {

  /** Finds a live candidate by id. */
  @Query("SELECT * FROM candidates WHERE id = :id AND deleted_at IS NULL")
  Mono<Candidate> findLiveById(UUID id);

  /** Resolves whether a live candidate already uses the normalized email within the tenant. */
  @Query(
      "SELECT EXISTS(SELECT 1 FROM candidates WHERE organization_id = :organizationId "
          + "AND lower(email) = :email AND deleted_at IS NULL)")
  Mono<Boolean> existsLiveByOrganizationAndEmail(UUID organizationId, String email);

  /** Lists the live candidates of an organization, newest first. */
  @Query(
      "SELECT * FROM candidates WHERE organization_id = :organizationId AND deleted_at IS NULL "
          + "ORDER BY created_at DESC")
  Flux<Candidate> listLiveByOrganization(UUID organizationId);

  /** Lists the live candidates of an organization in the given status, newest first. */
  @Query(
      "SELECT * FROM candidates WHERE organization_id = :organizationId AND status = :status "
          + "AND deleted_at IS NULL ORDER BY created_at DESC")
  Flux<Candidate> listLiveByOrganizationAndStatus(UUID organizationId, CandidateStatus status);

  /** Counts the live candidates of an organization. */
  @Query(
      "SELECT count(*) FROM candidates WHERE organization_id = :organizationId "
          + "AND deleted_at IS NULL")
  Mono<Long> countLiveByOrganization(UUID organizationId);
}
