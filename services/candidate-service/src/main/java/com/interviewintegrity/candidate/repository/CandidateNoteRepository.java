package com.interviewintegrity.candidate.repository;

import com.interviewintegrity.candidate.domain.CandidateNote;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link CandidateNote} entities. */
public interface CandidateNoteRepository extends ReactiveCrudRepository<CandidateNote, UUID> {

  /** Finds a live note by id. */
  @Query("SELECT * FROM candidate_notes WHERE id = :id AND deleted_at IS NULL")
  Mono<CandidateNote> findLiveById(UUID id);

  /** Lists the live notes of a candidate, newest first. */
  @Query(
      "SELECT * FROM candidate_notes WHERE organization_id = :organizationId "
          + "AND candidate_id = :candidateId AND deleted_at IS NULL ORDER BY created_at DESC")
  Flux<CandidateNote> listLiveByOrganizationAndCandidate(UUID organizationId, UUID candidateId);
}
