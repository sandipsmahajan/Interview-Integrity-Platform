package com.interviewintegrity.interview.service;

import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.interview.domain.Interviewer;
import com.interviewintegrity.interview.repository.InterviewerRepository;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages the interviewer profiles of an organization. */
public class InterviewerService {

  private final InterviewerRepository interviewerRepository;

  /** Wires the service with its repository. */
  public InterviewerService(InterviewerRepository interviewerRepository) {
    this.interviewerRepository = interviewerRepository;
  }

  /** Creates an interviewer profile, rejecting duplicates for the same user. */
  @Transactional
  public Mono<Interviewer> create(
      UUID organizationId, UUID userId, String fullName, String email, UUID createdBy) {
    return interviewerRepository
        .findLiveByOrganizationAndUser(organizationId, userId)
        .hasElement()
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(new ConflictException("Interviewer already exists for the user"));
              }
              return interviewerRepository.save(
                  new Interviewer(organizationId, userId, fullName, email, createdBy));
            });
  }

  /** Returns a single interviewer of the organization. */
  @Transactional(readOnly = true)
  public Mono<Interviewer> get(UUID id, UUID organizationId) {
    return requireOwned(id, organizationId);
  }

  /** Lists the live interviewers of an organization. */
  @Transactional(readOnly = true)
  public Flux<Interviewer> list(UUID organizationId) {
    return interviewerRepository.listLiveByOrganization(organizationId);
  }

  /** Updates an interviewer profile. */
  @Transactional
  public Mono<Interviewer> update(
      UUID id, UUID organizationId, String fullName, String email, UUID byUser) {
    return requireOwned(id, organizationId)
        .map(
            interviewer -> {
              interviewer.update(fullName, email, byUser);
              return interviewer;
            })
        .flatMap(interviewerRepository::save);
  }

  /** Soft deletes an interviewer profile. */
  @Transactional
  public Mono<Void> delete(UUID id, UUID organizationId, UUID byUser) {
    return requireOwned(id, organizationId)
        .map(
            interviewer -> {
              interviewer.delete(byUser);
              return interviewer;
            })
        .flatMap(interviewerRepository::save)
        .then();
  }

  private Mono<Interviewer> requireOwned(UUID id, UUID organizationId) {
    return interviewerRepository
        .findLiveById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Interviewer not found")))
        .flatMap(
            interviewer -> {
              if (!organizationId.equals(interviewer.getOrganizationId())) {
                return Mono.error(new NotFoundException("Interviewer not found"));
              }
              return Mono.just(interviewer);
            });
  }
}
