package com.integrity.candidate.repository;

import com.integrity.candidate.domain.CandidateDocument;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link CandidateDocument} entities. */
public interface CandidateDocumentRepository
    extends ReactiveCrudRepository<CandidateDocument, UUID> {

  /** Finds a live document by id. */
  @Query("SELECT * FROM candidate_documents WHERE id = :id AND deleted_at IS NULL")
  Mono<CandidateDocument> findLiveById(UUID id);

  /** Lists the live documents of a candidate, newest first. */
  @Query(
      "SELECT * FROM candidate_documents WHERE organization_id = :organizationId "
          + "AND candidate_id = :candidateId AND deleted_at IS NULL ORDER BY uploaded_at DESC")
  Flux<CandidateDocument> listLiveByOrganizationAndCandidate(UUID organizationId, UUID candidateId);
}
