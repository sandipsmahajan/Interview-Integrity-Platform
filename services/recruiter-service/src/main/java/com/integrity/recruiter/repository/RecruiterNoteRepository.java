package com.integrity.recruiter.repository;

import com.integrity.recruiter.domain.RecruiterNote;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link RecruiterNote} entities. */
public interface RecruiterNoteRepository extends ReactiveCrudRepository<RecruiterNote, UUID> {

  /** Finds a live note by id. */
  @Query("SELECT * FROM recruiter_notes WHERE id = :id AND deleted_at IS NULL")
  Mono<RecruiterNote> findLiveById(UUID id);

  /** Lists the live notes of a candidate ordered by creation time descending. */
  @Query(
      "SELECT * FROM recruiter_notes WHERE organization_id = :organizationId "
          + "AND candidate_id = :candidateId AND deleted_at IS NULL ORDER BY created_at DESC")
  Flux<RecruiterNote> listLiveByOrganizationAndCandidate(UUID organizationId, UUID candidateId);
}
