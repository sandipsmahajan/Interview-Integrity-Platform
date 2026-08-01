package com.interviewintegrity.candidate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.interviewintegrity.candidate.domain.Candidate;
import com.interviewintegrity.candidate.domain.CandidateProfile;
import com.interviewintegrity.candidate.repository.CandidateProfileRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the candidate extended profile service. */
class CandidateProfileServiceTest {

  private final CandidateProfileRepository profileRepository =
      Mockito.mock(CandidateProfileRepository.class);
  private final CandidateService candidateService = Mockito.mock(CandidateService.class);

  private CandidateProfileService profileService;

  @BeforeEach
  void setUp() {
    profileService = new CandidateProfileService(profileRepository, candidateService);
  }

  @Test
  void getOrCreateCreatesEmptyProfileOnFirstAccess() {
    UUID candidateId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    when(candidateService.requireCandidate(candidateId, organizationId))
        .thenReturn(Mono.just(candidate(candidateId, organizationId)));
    when(profileRepository.findByCandidateId(candidateId)).thenReturn(Mono.empty());
    when(profileRepository.save(any(CandidateProfile.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(profileService.getOrCreate(candidateId, organizationId))
        .assertNext(
            profile -> {
              assertThat(profile.getCandidateId()).isEqualTo(candidateId);
              assertThat(profile.getSkills()).isEmpty();
              assertThat(profile.getAttributes()).isEqualTo("{}");
            })
        .verifyComplete();
  }

  @Test
  void updateReplacesProfileFields() {
    UUID candidateId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    CandidateProfile existing = new CandidateProfile(candidateId, organizationId);
    existing.setId(UUID.randomUUID());
    when(candidateService.requireCandidate(candidateId, organizationId))
        .thenReturn(Mono.just(candidate(candidateId, organizationId)));
    when(profileRepository.findByCandidateId(candidateId)).thenReturn(Mono.just(existing));
    when(profileRepository.save(any(CandidateProfile.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            profileService.update(
                candidateId,
                organizationId,
                "Senior Engineer",
                "bio",
                "Berlin",
                "Europe/Berlin",
                "summary",
                "https://linkedin.com/x",
                "https://github.com/x",
                List.of("java", "spring"),
                new BigDecimal("5.5"),
                "{\"remote\":true}"))
        .assertNext(
            profile -> {
              assertThat(profile.getHeadline()).isEqualTo("Senior Engineer");
              assertThat(profile.getSkills()).containsExactly("java", "spring");
              assertThat(profile.getExperienceYears()).isEqualByComparingTo(new BigDecimal("5.5"));
            })
        .verifyComplete();
  }

  private static Candidate candidate(UUID candidateId, UUID organizationId) {
    Candidate candidate =
        new Candidate(organizationId, null, "a@b.com", "Jane", null, null, UUID.randomUUID());
    candidate.setId(candidateId);
    return candidate;
  }
}
