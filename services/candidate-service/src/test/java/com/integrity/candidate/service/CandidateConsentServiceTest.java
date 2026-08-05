package com.integrity.candidate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.integrity.candidate.domain.Candidate;
import com.integrity.candidate.domain.CandidateConsent;
import com.integrity.candidate.domain.ConsentStatus;
import com.integrity.candidate.repository.CandidateConsentRepository;
import com.integrity.exception.ConflictException;
import com.integrity.exception.NotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the candidate consent service. */
class CandidateConsentServiceTest {

  private static final String CONSENT_TYPE = "monitoring";
  private static final String CONSENT_VERSION = "1.0";

  private final CandidateConsentRepository consentRepository =
      Mockito.mock(CandidateConsentRepository.class);
  private final CandidateService candidateService = Mockito.mock(CandidateService.class);

  private CandidateConsentService consentService;

  @BeforeEach
  void setUp() {
    consentService = new CandidateConsentService(consentRepository, candidateService);
  }

  @Test
  void grantCreatesConsentWhenNoneExists() {
    UUID organizationId = UUID.randomUUID();
    UUID candidateId = UUID.randomUUID();
    UUID grantedBy = UUID.randomUUID();
    when(candidateService.requireCandidate(candidateId, organizationId))
        .thenReturn(Mono.just(candidate(candidateId, organizationId)));
    when(consentRepository.findByOrganizationAndCandidateAndType(
            organizationId, candidateId, CONSENT_TYPE))
        .thenReturn(Mono.empty());
    when(consentRepository.save(any(CandidateConsent.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            consentService.grant(
                organizationId, candidateId, CONSENT_TYPE, CONSENT_VERSION, grantedBy))
        .assertNext(
            consent -> {
              assertThat(consent.getConsentType()).isEqualTo(CONSENT_TYPE);
              assertThat(consent.getConsentVersion()).isEqualTo(CONSENT_VERSION);
              assertThat(consent.getStatus()).isEqualTo(ConsentStatus.GRANTED);
            })
        .verifyComplete();
  }

  @Test
  void grantRejectsDuplicateConsentType() {
    UUID organizationId = UUID.randomUUID();
    UUID candidateId = UUID.randomUUID();
    CandidateConsent existing = consent(organizationId, candidateId);
    when(candidateService.requireCandidate(candidateId, organizationId))
        .thenReturn(Mono.just(candidate(candidateId, organizationId)));
    when(consentRepository.findByOrganizationAndCandidateAndType(
            organizationId, candidateId, CONSENT_TYPE))
        .thenReturn(Mono.just(existing));

    StepVerifier.create(
            consentService.grant(organizationId, candidateId, CONSENT_TYPE, CONSENT_VERSION, null))
        .expectError(ConflictException.class)
        .verify();
  }

  @Test
  void revokeWithdrawsConsent() {
    UUID consentId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    UUID candidateId = UUID.randomUUID();
    CandidateConsent consent = consent(organizationId, candidateId);
    consent.setId(consentId);
    when(consentRepository.findById(consentId)).thenReturn(Mono.just(consent));
    when(consentRepository.save(any(CandidateConsent.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(consentService.revoke(consentId, organizationId, UUID.randomUUID()))
        .assertNext(revoked -> assertThat(revoked.getStatus()).isEqualTo(ConsentStatus.REVOKED))
        .verifyComplete();
  }

  @Test
  void revokeReturnsNotFoundForUnknownConsent() {
    UUID consentId = UUID.randomUUID();
    when(consentRepository.findById(consentId)).thenReturn(Mono.empty());

    StepVerifier.create(consentService.revoke(consentId, UUID.randomUUID(), UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  private static CandidateConsent consent(UUID organizationId, UUID candidateId) {
    return new CandidateConsent(
        organizationId, candidateId, CONSENT_TYPE, CONSENT_VERSION, UUID.randomUUID());
  }

  private static Candidate candidate(UUID candidateId, UUID organizationId) {
    Candidate candidate =
        new Candidate(organizationId, null, "a@b.com", "Jane", null, null, UUID.randomUUID());
    candidate.setId(candidateId);
    return candidate;
  }
}
