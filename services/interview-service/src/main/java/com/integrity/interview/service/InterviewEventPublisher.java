package com.integrity.interview.service;

import com.integrity.interview.domain.Interview;
import com.integrity.interview.domain.InterviewSession;
import reactor.core.publisher.Mono;

/** Publishes interview domain events onto the platform event bus. */
public interface InterviewEventPublisher {

  /**
   * Publishes the interview creation event.
   *
   * @param interview the created interview
   * @return completion signal of the publish attempt
   */
  Mono<Void> publishCreated(Interview interview);

  /**
   * Publishes the interview scheduling event.
   *
   * @param interview the scheduled interview
   * @return completion signal of the publish attempt
   */
  Mono<Void> publishScheduled(Interview interview);

  /**
   * Publishes the interview start event.
   *
   * @param session the session that started the interview
   * @return completion signal of the publish attempt
   */
  Mono<Void> publishStarted(InterviewSession session);

  /**
   * Publishes the interview completion event.
   *
   * @param session the session that completed the interview
   * @return completion signal of the publish attempt
   */
  Mono<Void> publishCompleted(InterviewSession session);
}
