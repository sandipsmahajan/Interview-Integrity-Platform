package com.interviewintegrity.candidate.web.dto;

import com.interviewintegrity.candidate.domain.ConsentStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a candidate consent.
 *
 * @param id consent identifier
 * @param candidateId consenting candidate
 * @param consentType type of the consent
 * @param status grant state
 * @param grantedAt instant the consent was granted
 * @param grantedBy user that granted the consent
 * @param revokedAt instant the consent was revoked
 * @param revokedBy user that revoked the consent
 * @param consentVersion version of the consent text
 */
public record CandidateConsentResponse(
    UUID id,
    UUID candidateId,
    String consentType,
    ConsentStatus status,
    Instant grantedAt,
    UUID grantedBy,
    Instant revokedAt,
    UUID revokedBy,
    String consentVersion) {}
