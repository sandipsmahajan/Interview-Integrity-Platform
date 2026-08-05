package com.integrity.recruiter.service;

import com.integrity.exception.ConflictException;
import com.integrity.exception.NotFoundException;
import com.integrity.recruiter.domain.Recruiter;
import com.integrity.recruiter.domain.RecruiterStatus;
import com.integrity.recruiter.repository.RecruiterRepository;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages the recruiter profiles of an organization. */
public class RecruiterService {

  private static final String RECRUITER_NOT_FOUND = "Recruiter not found";

  private final RecruiterRepository recruiterRepository;

  /** Wires the service with its repository. */
  public RecruiterService(RecruiterRepository recruiterRepository) {
    this.recruiterRepository = recruiterRepository;
  }

  /** Creates a recruiter profile, rejecting duplicates for the same user. */
  @Transactional
  public Mono<Recruiter> createRecruiter(
      UUID organizationId,
      UUID userId,
      String fullName,
      String email,
      String title,
      UUID createdBy) {
    return recruiterRepository
        .findLiveByOrganizationAndUser(organizationId, userId)
        .hasElement()
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(new ConflictException("Recruiter already exists for the user"));
              }
              return recruiterRepository.save(
                  new Recruiter(organizationId, userId, fullName, email, title, createdBy));
            });
  }

  /** Returns the recruiter linked to a user within an organization. */
  @Transactional(readOnly = true)
  public Mono<Recruiter> getByUser(UUID organizationId, UUID userId) {
    return recruiterRepository
        .findLiveByOrganizationAndUser(organizationId, userId)
        .switchIfEmpty(Mono.error(new NotFoundException("Recruiter profile not found")));
  }

  /** Returns a single recruiter by id. */
  @Transactional(readOnly = true)
  public Mono<Recruiter> getById(UUID id) {
    return recruiterRepository
        .findLiveById(id)
        .switchIfEmpty(Mono.error(new NotFoundException(RECRUITER_NOT_FOUND)));
  }

  /** Lists the recruiters of an organization, optionally filtered by status. */
  @Transactional(readOnly = true)
  public Flux<Recruiter> list(UUID organizationId, RecruiterStatus status) {
    if (status == null) {
      return recruiterRepository.listLiveByOrganization(organizationId);
    }
    return recruiterRepository.listLiveByOrganizationAndStatus(organizationId, status);
  }

  /** Updates the profile fields of a recruiter. */
  @Transactional
  public Mono<Recruiter> update(
      UUID id, UUID organizationId, String fullName, String email, String title, UUID byUser) {
    return recruiterRepository
        .findLiveById(id)
        .switchIfEmpty(Mono.error(new NotFoundException(RECRUITER_NOT_FOUND)))
        .flatMap(recruiter -> assertOrganization(recruiter, organizationId))
        .map(
            recruiter -> {
              recruiter.updateProfile(fullName, email, title, byUser);
              return recruiter;
            })
        .flatMap(recruiterRepository::save);
  }

  /** Changes the working status of a recruiter. */
  @Transactional
  public Mono<Recruiter> changeStatus(
      UUID id, UUID organizationId, RecruiterStatus status, UUID byUser) {
    return recruiterRepository
        .findLiveById(id)
        .switchIfEmpty(Mono.error(new NotFoundException(RECRUITER_NOT_FOUND)))
        .flatMap(recruiter -> assertOrganization(recruiter, organizationId))
        .map(
            recruiter -> {
              recruiter.changeStatus(status, byUser);
              return recruiter;
            })
        .flatMap(recruiterRepository::save);
  }

  /** Soft deletes a recruiter profile. */
  @Transactional
  public Mono<Void> delete(UUID id, UUID organizationId, UUID byUser) {
    return recruiterRepository
        .findLiveById(id)
        .switchIfEmpty(Mono.error(new NotFoundException(RECRUITER_NOT_FOUND)))
        .flatMap(recruiter -> assertOrganization(recruiter, organizationId))
        .map(
            recruiter -> {
              recruiter.delete(byUser);
              return recruiter;
            })
        .flatMap(recruiterRepository::save)
        .then();
  }

  private Mono<Recruiter> assertOrganization(Recruiter recruiter, UUID organizationId) {
    return Mono.justOrEmpty(recruiter)
        .flatMap(
            r -> {
              if (!organizationId.equals(r.getOrganizationId())) {
                return Mono.error(new NotFoundException(RECRUITER_NOT_FOUND));
              }
              return Mono.just(r);
            });
  }
}
