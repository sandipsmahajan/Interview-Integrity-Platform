package com.integrity.candidate.repository;

import com.integrity.candidate.domain.CandidateConsent;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link CandidateConsent} entities. */
public interface CandidateConsentRepository extends ReactiveCrudRepository<CandidateConsent, UUID> {

  /** Lists the consents of a candidate, most recently granted first. */
  @Query(
      "SELECT * FROM candidate_consents WHERE organization_id = :organizationId "
          + "AND candidate_id = :candidateId ORDER BY granted_at DESC")
  Flux<CandidateConsent> listByOrganizationAndCandidate(UUID organizationId, UUID candidateId);

  /** Finds the consent of a candidate for a consent type. */
  @Query(
      "SELECT * FROM candidate_consents WHERE organization_id = :organizationId "
          + "AND candidate_id = :candidateId AND consent_type = :consentType LIMIT 1")
  Mono<CandidateConsent> findByOrganizationAndCandidateAndType(
      UUID organizationId, UUID candidateId, String consentType);
}
