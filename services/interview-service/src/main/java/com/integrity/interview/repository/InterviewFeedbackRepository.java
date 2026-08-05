package com.integrity.interview.repository;

import com.integrity.interview.domain.InterviewFeedback;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link InterviewFeedback} entities. */
public interface InterviewFeedbackRepository
    extends ReactiveCrudRepository<InterviewFeedback, UUID> {

  /** Finds a live feedback record by id. */
  @Query("SELECT * FROM interview_feedback WHERE id = :id AND deleted_at IS NULL")
  Mono<InterviewFeedback> findLiveById(UUID id);

  /** Lists the live feedback of an interview, newest first. */
  @Query(
      "SELECT * FROM interview_feedback WHERE organization_id = :organizationId "
          + "AND interview_id = :interviewId AND deleted_at IS NULL ORDER BY created_at DESC")
  Flux<InterviewFeedback> listLiveByOrganizationAndInterview(UUID organizationId, UUID interviewId);

  /** Finds the live feedback of an interviewer for an interview. */
  @Query(
      "SELECT * FROM interview_feedback WHERE organization_id = :organizationId "
          + "AND interview_id = :interviewId AND interviewer_id = :interviewerId "
          + "AND deleted_at IS NULL LIMIT 1")
  Mono<InterviewFeedback> findLiveByOrganizationAndInterviewAndInterviewer(
      UUID organizationId, UUID interviewId, UUID interviewerId);
}
