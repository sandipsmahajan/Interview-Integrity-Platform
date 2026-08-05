package com.integrity.interview.repository;

import com.integrity.interview.domain.InterviewSession;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link InterviewSession} entities. */
public interface InterviewSessionRepository extends ReactiveCrudRepository<InterviewSession, UUID> {

  /** Finds a session by id. */
  @Override
  Mono<InterviewSession> findById(UUID id);

  /** Lists the sessions of an interview, newest first. */
  @Query(
      "SELECT * FROM interview_sessions WHERE organization_id = :organizationId "
          + "AND interview_id = :interviewId ORDER BY created_at DESC")
  Flux<InterviewSession> listByOrganizationAndInterview(UUID organizationId, UUID interviewId);

  /** Finds the active or paused session of an interview, when any. */
  @Query(
      "SELECT * FROM interview_sessions WHERE organization_id = :organizationId "
          + "AND interview_id = :interviewId AND status IN ('ACTIVE', 'PAUSED') LIMIT 1")
  Mono<InterviewSession> findActiveByInterview(UUID organizationId, UUID interviewId);
}
