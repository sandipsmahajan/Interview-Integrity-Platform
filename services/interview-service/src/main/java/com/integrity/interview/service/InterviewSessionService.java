package com.integrity.interview.service;

import com.integrity.exception.ConflictException;
import com.integrity.exception.NotFoundException;
import com.integrity.interview.domain.Interview;
import com.integrity.interview.domain.InterviewSession;
import com.integrity.interview.repository.InterviewRepository;
import com.integrity.interview.repository.InterviewSessionRepository;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages the monitoring sessions of interviews. */
public class InterviewSessionService {

  private final InterviewSessionRepository sessionRepository;
  private final InterviewRepository interviewRepository;
  private final InterviewEventPublisher eventPublisher;

  /** Wires the service with its repositories and event publisher. */
  public InterviewSessionService(
      InterviewSessionRepository sessionRepository,
      InterviewRepository interviewRepository,
      InterviewEventPublisher eventPublisher) {
    this.sessionRepository = sessionRepository;
    this.interviewRepository = interviewRepository;
    this.eventPublisher = eventPublisher;
  }

  /** Starts a monitoring session for an interview and publishes the start event. */
  @Transactional
  public Mono<InterviewSession> start(
      UUID organizationId,
      UUID interviewId,
      String sessionTokenHash,
      String deviceId,
      String clientVersion,
      int heartbeatCadenceSeconds,
      UUID byUser) {
    return requireInterview(organizationId, interviewId)
        .flatMap(
            interview ->
                sessionRepository
                    .findActiveByInterview(organizationId, interviewId)
                    .hasElement()
                    .flatMap(
                        active -> {
                          if (active) {
                            return Mono.error(
                                new ConflictException("Interview already has an active session"));
                          }
                          return sessionRepository.save(
                              new InterviewSession(
                                  organizationId,
                                  interviewId,
                                  sessionTokenHash,
                                  deviceId,
                                  clientVersion,
                                  heartbeatCadenceSeconds));
                        }))
        .flatMap(
            session -> {
              session.start();
              return sessionRepository.save(session);
            })
        .flatMap(
            session ->
                interviewRepository
                    .findLiveById(interviewId)
                    .flatMap(
                        interview -> {
                          interview.markInProgress(byUser);
                          return interviewRepository.save(interview);
                        })
                    .then(eventPublisher.publishStarted(session))
                    .thenReturn(session));
  }

  /** Lists the sessions of an interview. */
  @Transactional(readOnly = true)
  public Flux<InterviewSession> list(UUID organizationId, UUID interviewId) {
    return requireInterview(organizationId, interviewId)
        .thenMany(sessionRepository.listByOrganizationAndInterview(organizationId, interviewId));
  }

  /** Pauses an active session. */
  @Transactional
  public Mono<InterviewSession> pause(UUID sessionId, UUID organizationId) {
    return requireOwnedSession(sessionId, organizationId)
        .map(
            session -> {
              session.pause();
              return session;
            })
        .flatMap(sessionRepository::save);
  }

  /** Resumes a paused session. */
  @Transactional
  public Mono<InterviewSession> resume(UUID sessionId, UUID organizationId) {
    return requireOwnedSession(sessionId, organizationId)
        .map(
            session -> {
              session.resume();
              return session;
            })
        .flatMap(sessionRepository::save);
  }

  /** Completes a session, completes its interview and publishes the completion event. */
  @Transactional
  public Mono<InterviewSession> complete(UUID sessionId, UUID organizationId, UUID byUser) {
    return requireOwnedSession(sessionId, organizationId)
        .flatMap(
            session -> {
              session.complete();
              return sessionRepository.save(session);
            })
        .flatMap(
            session ->
                interviewRepository
                    .findLiveById(session.getInterviewId())
                    .flatMap(
                        interview -> {
                          interview.complete(byUser);
                          return interviewRepository.save(interview);
                        })
                    .then(eventPublisher.publishCompleted(session))
                    .thenReturn(session));
  }

  /** Marks a session as ended abnormally. */
  @Transactional
  public Mono<InterviewSession> markAbnormal(UUID sessionId, UUID organizationId) {
    return requireOwnedSession(sessionId, organizationId)
        .map(
            session -> {
              session.markAbnormal();
              return session;
            })
        .flatMap(sessionRepository::save);
  }

  private Mono<Interview> requireInterview(UUID organizationId, UUID interviewId) {
    return interviewRepository
        .findLiveById(interviewId)
        .switchIfEmpty(Mono.error(new NotFoundException("Interview not found")))
        .flatMap(
            interview -> {
              if (!organizationId.equals(interview.getOrganizationId())) {
                return Mono.error(new NotFoundException("Interview not found"));
              }
              return Mono.just(interview);
            });
  }

  private Mono<InterviewSession> requireOwnedSession(UUID sessionId, UUID organizationId) {
    return sessionRepository
        .findById(sessionId)
        .switchIfEmpty(Mono.error(new NotFoundException("Session not found")))
        .flatMap(
            session -> {
              if (!organizationId.equals(session.getOrganizationId())) {
                return Mono.error(new NotFoundException("Session not found"));
              }
              return Mono.just(session);
            });
  }
}
