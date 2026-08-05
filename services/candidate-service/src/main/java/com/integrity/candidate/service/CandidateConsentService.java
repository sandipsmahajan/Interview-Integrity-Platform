package com.integrity.candidate.service;

import com.integrity.candidate.domain.CandidateConsent;
import com.integrity.candidate.domain.ConsentStatus;
import com.integrity.candidate.repository.CandidateConsentRepository;
import com.integrity.exception.ConflictException;
import com.integrity.exception.NotFoundException;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages data-protection consents granted by candidates. */
public class CandidateConsentService {

  private final CandidateConsentRepository consentRepository;
  private final CandidateService candidateService;

  /** Wires the service with its repository and the candidate service. */
  public CandidateConsentService(
      CandidateConsentRepository consentRepository, CandidateService candidateService) {
    this.consentRepository = consentRepository;
    this.candidateService = candidateService;
  }

  /** Grants a consent to a candidate, rejecting an existing consent of the same type. */
  @Transactional
  public Mono<CandidateConsent> grant(
      UUID organizationId,
      UUID candidateId,
      String consentType,
      String consentVersion,
      UUID grantedBy) {
    return candidateService
        .requireCandidate(candidateId, organizationId)
        .then(
            consentRepository
                .findByOrganizationAndCandidateAndType(organizationId, candidateId, consentType)
                .hasElement())
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(
                    new ConflictException("Consent already exists for type " + consentType));
              }
              return consentRepository.save(
                  new CandidateConsent(
                      organizationId, candidateId, consentType, consentVersion, grantedBy));
            });
  }

  /** Lists the consents of a candidate. */
  @Transactional(readOnly = true)
  public Flux<CandidateConsent> list(UUID organizationId, UUID candidateId) {
    return candidateService
        .requireCandidate(candidateId, organizationId)
        .thenMany(consentRepository.listByOrganizationAndCandidate(organizationId, candidateId));
  }

  /** Withdraws an active consent. */
  @Transactional
  public Mono<CandidateConsent> revoke(UUID consentId, UUID organizationId, UUID revokedBy) {
    return consentRepository
        .findById(consentId)
        .switchIfEmpty(Mono.error(new NotFoundException("Consent not found")))
        .flatMap(consent -> assertOrganization(consent, organizationId))
        .flatMap(
            consent -> {
              if (consent.getStatus() == ConsentStatus.REVOKED) {
                return Mono.error(new ConflictException("Consent already revoked"));
              }
              consent.revoke(revokedBy);
              return consentRepository.save(consent);
            });
  }

  private Mono<CandidateConsent> assertOrganization(CandidateConsent consent, UUID organizationId) {
    if (!organizationId.equals(consent.getOrganizationId())) {
      return Mono.error(new NotFoundException("Consent not found"));
    }
    return Mono.just(consent);
  }
}
