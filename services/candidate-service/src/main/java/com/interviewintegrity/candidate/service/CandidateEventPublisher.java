package com.interviewintegrity.candidate.service;

import com.interviewintegrity.candidate.domain.Candidate;
import reactor.core.publisher.Mono;

/** Publishes candidate domain events onto the platform event bus. */
public interface CandidateEventPublisher {

  /**
   * Publishes the candidate registration event.
   *
   * @param candidate the newly created candidate
   * @return completion signal of the publish attempt
   */
  Mono<Void> publishCandidateRegistered(Candidate candidate);
}
