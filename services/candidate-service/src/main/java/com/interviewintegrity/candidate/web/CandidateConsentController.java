package com.interviewintegrity.candidate.web;

import com.interviewintegrity.candidate.domain.CandidateConsent;
import com.interviewintegrity.candidate.service.CandidateConsentService;
import com.interviewintegrity.candidate.web.dto.CandidateConsentResponse;
import com.interviewintegrity.candidate.web.dto.GrantConsentRequest;
import com.interviewintegrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Consent endpoints scoped to a candidate. */
@RestController
@RequestMapping("/api/v1/candidates/{candidateId}/consents")
@Tag(name = "Candidate Consents", description = "Manage data-protection consents")
public final class CandidateConsentController {

  private final CandidateConsentService consentService;

  /** Creates the controller bound to the consent service. */
  public CandidateConsentController(CandidateConsentService consentService) {
    this.consentService = consentService;
  }

  /** Grants a consent to a candidate. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Grant a consent to a candidate")
  public Mono<CandidateConsentResponse> grant(
      Authentication authentication,
      @PathVariable UUID candidateId,
      @Valid @RequestBody GrantConsentRequest request) {
    return consentService
        .grant(
            SecurityPrincipals.organizationId(authentication),
            candidateId,
            request.consentType().trim(),
            request.consentVersion(),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  /** Lists the consents of a candidate. */
  @GetMapping
  @Operation(summary = "List candidate consents")
  public Flux<CandidateConsentResponse> list(
      Authentication authentication, @PathVariable UUID candidateId) {
    return consentService
        .list(SecurityPrincipals.organizationId(authentication), candidateId)
        .map(this::toResponse);
  }

  /** Withdraws a consent. */
  @PostMapping("/{consentId}/revoke")
  @Operation(summary = "Revoke a candidate consent")
  public Mono<CandidateConsentResponse> revoke(
      Authentication authentication, @PathVariable UUID consentId) {
    return consentService
        .revoke(
            consentId,
            SecurityPrincipals.organizationId(authentication),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  private CandidateConsentResponse toResponse(CandidateConsent consent) {
    return new CandidateConsentResponse(
        consent.getId(),
        consent.getCandidateId(),
        consent.getConsentType(),
        consent.getStatus(),
        consent.getGrantedAt(),
        consent.getGrantedBy(),
        consent.getRevokedAt(),
        consent.getRevokedBy(),
        consent.getConsentVersion());
  }
}
