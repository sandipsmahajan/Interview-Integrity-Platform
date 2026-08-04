package com.interviewintegrity.candidate.service;

import com.interviewintegrity.candidate.domain.Candidate;
import com.interviewintegrity.candidate.domain.CandidateStatus;
import com.interviewintegrity.candidate.repository.CandidateRepository;
import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.exception.NotFoundException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages the candidate master records of an organization. */
public class CandidateService {

  private final CandidateRepository candidateRepository;
  private final CandidateEventPublisher eventPublisher;

  /** Wires the service with its repository and event publisher. */
  public CandidateService(
      CandidateRepository candidateRepository, CandidateEventPublisher eventPublisher) {
    this.candidateRepository = candidateRepository;
    this.eventPublisher = eventPublisher;
  }

  /** Creates a candidate, rejecting duplicate emails for the same tenant. */
  @Transactional
  public Mono<Candidate> create(
      UUID organizationId,
      UUID userId,
      String email,
      String fullName,
      String phone,
      String source,
      UUID createdBy) {
    String normalizedEmail = email.toLowerCase(Locale.ROOT);
    return candidateRepository
        .existsLiveByOrganizationAndEmail(organizationId, normalizedEmail)
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(
                    new ConflictException("Candidate already exists with this email"));
              }
              return candidateRepository.save(
                  new Candidate(
                      organizationId,
                      userId,
                      normalizedEmail,
                      fullName.trim(),
                      phone,
                      source,
                      createdBy));
            })
        .flatMap(
            candidate ->
                eventPublisher.publishCandidateRegistered(candidate).thenReturn(candidate));
  }

  /** Returns a single live candidate scoped to the tenant. */
  @Transactional(readOnly = true)
  public Mono<Candidate> getById(UUID id, UUID organizationId) {
    return requireCandidate(id, organizationId);
  }

  /** Verifies a live candidate exists within the tenant and returns it. */
  @Transactional(readOnly = true)
  public Mono<Candidate> requireCandidate(UUID id, UUID organizationId) {
    return candidateRepository
        .findLiveById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Candidate not found")))
        .flatMap(candidate -> assertOrganization(candidate, organizationId));
  }

  /** Lists the live candidates of an organization, optionally filtered by status. */
  @Transactional(readOnly = true)
  public Flux<Candidate> list(UUID organizationId, CandidateStatus status) {
    if (status == null) {
      return candidateRepository.listLiveByOrganization(organizationId);
    }
    return candidateRepository.listLiveByOrganizationAndStatus(organizationId, status);
  }

  /** Updates the mutable fields of a candidate. */
  @Transactional
  public Mono<Candidate> update(
      UUID id, UUID organizationId, String fullName, String phone, String source, UUID byUser) {
    return requireCandidate(id, organizationId)
        .map(
            candidate -> {
              candidate.update(fullName.trim(), phone, source, byUser);
              return candidate;
            })
        .flatMap(candidateRepository::save);
  }

  /** Changes the lifecycle status of a candidate. */
  @Transactional
  public Mono<Candidate> changeStatus(
      UUID id, UUID organizationId, CandidateStatus status, UUID byUser) {
    return requireCandidate(id, organizationId)
        .map(
            candidate -> {
              candidate.changeStatus(status, byUser);
              return candidate;
            })
        .flatMap(candidateRepository::save);
  }

  /** Soft deletes a candidate. */
  @Transactional
  public Mono<Void> delete(UUID id, UUID organizationId, UUID byUser) {
    return requireCandidate(id, organizationId)
        .map(
            candidate -> {
              candidate.delete(byUser);
              return candidate;
            })
        .flatMap(candidateRepository::save)
        .then();
  }

  private Mono<Candidate> assertOrganization(Candidate candidate, UUID organizationId) {
    if (!organizationId.equals(candidate.getOrganizationId())) {
      return Mono.error(new NotFoundException("Candidate not found"));
    }
    return Mono.just(candidate);
  }
}
