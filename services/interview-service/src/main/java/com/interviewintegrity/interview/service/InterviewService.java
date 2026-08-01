package com.interviewintegrity.interview.service;

import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.interview.domain.Interview;
import com.interviewintegrity.interview.domain.InterviewMode;
import com.interviewintegrity.interview.domain.InterviewStatus;
import com.interviewintegrity.interview.repository.InterviewRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages the interview records of an organization. */
public class InterviewService {

  private final InterviewRepository interviewRepository;
  private final InterviewEventPublisher eventPublisher;

  /** Wires the service with its repository and event publisher. */
  public InterviewService(
      InterviewRepository interviewRepository, InterviewEventPublisher eventPublisher) {
    this.interviewRepository = interviewRepository;
    this.eventPublisher = eventPublisher;
  }

  /** Creates an interview and publishes the creation event. */
  @Transactional
  public Mono<Interview> create(
      UUID organizationId,
      UUID candidateId,
      UUID recruiterId,
      int roundNumber,
      String title,
      InterviewMode mode,
      String meetingUrl,
      Instant startsAt,
      Instant endsAt,
      String timezone,
      String metadata,
      UUID createdBy) {
    return interviewRepository
        .save(
            new Interview(
                organizationId,
                candidateId,
                recruiterId,
                roundNumber,
                title,
                mode,
                meetingUrl,
                startsAt,
                endsAt,
                timezone,
                metadata,
                createdBy))
        .flatMap(interview -> eventPublisher.publishCreated(interview).thenReturn(interview));
  }

  /** Returns a single interview of the organization. */
  @Transactional(readOnly = true)
  public Mono<Interview> get(UUID id, UUID organizationId) {
    return requireOwned(id, organizationId);
  }

  /** Lists the live interviews of an organization, optionally filtered by status. */
  @Transactional(readOnly = true)
  public Flux<Interview> list(UUID organizationId, InterviewStatus status) {
    if (status == null) {
      return interviewRepository.listLiveByOrganization(organizationId);
    }
    return interviewRepository.listLiveByOrganizationAndStatus(organizationId, status);
  }

  /** Lists the live interviews of an organization for a candidate. */
  @Transactional(readOnly = true)
  public Flux<Interview> listByCandidate(UUID organizationId, UUID candidateId) {
    return interviewRepository.listLiveByOrganizationAndCandidate(organizationId, candidateId);
  }

  /** Lists the live interviews of an organization for a recruiter. */
  @Transactional(readOnly = true)
  public Flux<Interview> listByRecruiter(UUID organizationId, UUID recruiterId) {
    return interviewRepository.listLiveByOrganizationAndRecruiter(organizationId, recruiterId);
  }

  /** Re-schedules an interview and publishes the scheduling event. */
  @Transactional
  public Mono<Interview> schedule(
      UUID id,
      UUID organizationId,
      Instant startsAt,
      Instant endsAt,
      String timezone,
      String meetingUrl,
      UUID byUser) {
    return requireOwned(id, organizationId)
        .map(
            interview -> {
              interview.schedule(startsAt, endsAt, timezone, meetingUrl, byUser);
              return interview;
            })
        .flatMap(interviewRepository::save)
        .flatMap(interview -> eventPublisher.publishScheduled(interview).thenReturn(interview));
  }

  /** Updates the mutable details of an interview. */
  @Transactional
  public Mono<Interview> update(
      UUID id, UUID organizationId, String title, String meetingUrl, String metadata, UUID byUser) {
    return requireOwned(id, organizationId)
        .map(
            interview -> {
              interview.update(title, meetingUrl, metadata, byUser);
              return interview;
            })
        .flatMap(interviewRepository::save);
  }

  /** Cancels an interview. */
  @Transactional
  public Mono<Interview> cancel(UUID id, UUID organizationId, UUID byUser) {
    return requireOwned(id, organizationId)
        .map(
            interview -> {
              interview.cancel(byUser);
              return interview;
            })
        .flatMap(interviewRepository::save);
  }

  /** Marks an interview as no-show. */
  @Transactional
  public Mono<Interview> markNoShow(UUID id, UUID organizationId, UUID byUser) {
    return requireOwned(id, organizationId)
        .map(
            interview -> {
              interview.markNoShow(byUser);
              return interview;
            })
        .flatMap(interviewRepository::save);
  }

  /** Soft deletes an interview. */
  @Transactional
  public Mono<Void> delete(UUID id, UUID organizationId, UUID byUser) {
    return requireOwned(id, organizationId)
        .map(
            interview -> {
              interview.delete(byUser);
              return interview;
            })
        .flatMap(interviewRepository::save)
        .then();
  }

  private Mono<Interview> requireOwned(UUID id, UUID organizationId) {
    return interviewRepository
        .findLiveById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Interview not found")))
        .flatMap(
            interview -> {
              if (!organizationId.equals(interview.getOrganizationId())) {
                return Mono.error(new NotFoundException("Interview not found"));
              }
              return Mono.just(interview);
            });
  }
}
