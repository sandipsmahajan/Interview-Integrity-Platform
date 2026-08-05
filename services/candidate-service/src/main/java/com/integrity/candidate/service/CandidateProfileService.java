package com.integrity.candidate.service;

import com.integrity.candidate.domain.CandidateProfile;
import com.integrity.candidate.repository.CandidateProfileRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/** Manages the extended profile of a candidate. */
public class CandidateProfileService {

  private final CandidateProfileRepository profileRepository;
  private final CandidateService candidateService;

  /** Wires the service with its repository and the candidate service. */
  public CandidateProfileService(
      CandidateProfileRepository profileRepository, CandidateService candidateService) {
    this.profileRepository = profileRepository;
    this.candidateService = candidateService;
  }

  /** Returns the extended profile of a candidate, creating an empty one on first access. */
  @Transactional
  public Mono<CandidateProfile> getOrCreate(UUID candidateId, UUID organizationId) {
    return candidateService
        .requireCandidate(candidateId, organizationId)
        .then(profileRepository.findByCandidateId(candidateId))
        .switchIfEmpty(
            Mono.defer(
                () -> profileRepository.save(new CandidateProfile(candidateId, organizationId))));
  }

  /** Updates the extended profile of a candidate. */
  @Transactional
  public Mono<CandidateProfile> update(
      UUID candidateId,
      UUID organizationId,
      String headline,
      String bio,
      String location,
      String timezone,
      String resumeSummary,
      String linkedinUrl,
      String githubUrl,
      List<String> skills,
      BigDecimal experienceYears,
      String attributes) {
    return getOrCreate(candidateId, organizationId)
        .flatMap(
            profile -> {
              profile.update(
                  headline,
                  bio,
                  location,
                  timezone,
                  resumeSummary,
                  linkedinUrl,
                  githubUrl,
                  skills,
                  experienceYears,
                  attributes);
              return profileRepository.save(profile);
            });
  }
}
