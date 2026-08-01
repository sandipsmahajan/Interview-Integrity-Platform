package com.interviewintegrity.recruiter.service;

import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.recruiter.domain.RecruiterProfile;
import com.interviewintegrity.recruiter.repository.RecruiterProfileRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/** Manages the extended profile of a recruiter. */
public class RecruiterProfileService {

  private final RecruiterProfileRepository profileRepository;

  /** Wires the service with its repository. */
  public RecruiterProfileService(RecruiterProfileRepository profileRepository) {
    this.profileRepository = profileRepository;
  }

  /** Returns the extended profile of a recruiter, creating an empty one on first access. */
  @Transactional
  public Mono<RecruiterProfile> getOrCreate(UUID recruiterId, UUID organizationId) {
    return profileRepository
        .findByRecruiterId(recruiterId)
        .switchIfEmpty(
            Mono.defer(
                () -> profileRepository.save(new RecruiterProfile(recruiterId, organizationId))));
  }

  /** Updates the extended profile of a recruiter. */
  @Transactional
  public Mono<RecruiterProfile> update(
      UUID recruiterId,
      UUID organizationId,
      String bio,
      List<String> specialties,
      String linkedinUrl,
      String availability) {
    String[] specialtyArray = specialties == null ? null : specialties.toArray(new String[0]);
    return getOrCreate(recruiterId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Recruiter profile not found")))
        .map(
            profile -> {
              profile.update(bio, specialtyArray, linkedinUrl, availability);
              return profile;
            })
        .flatMap(profileRepository::save);
  }
}
