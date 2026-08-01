package com.interviewintegrity.recruiter.repository;

import com.interviewintegrity.recruiter.domain.RecruiterProfile;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link RecruiterProfile} entities. */
public interface RecruiterProfileRepository extends ReactiveCrudRepository<RecruiterProfile, UUID> {

  /** Finds the extended profile of a recruiter. */
  Mono<RecruiterProfile> findByRecruiterId(UUID recruiterId);
}
