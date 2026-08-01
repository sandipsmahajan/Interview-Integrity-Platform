package com.interviewintegrity.candidate.repository;

import com.interviewintegrity.candidate.domain.CandidateProfile;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link CandidateProfile} entities. */
public interface CandidateProfileRepository extends ReactiveCrudRepository<CandidateProfile, UUID> {

  /** Finds the extended profile of a candidate. */
  Mono<CandidateProfile> findByCandidateId(UUID candidateId);
}
