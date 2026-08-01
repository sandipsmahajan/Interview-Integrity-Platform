package com.interviewintegrity.candidate.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to grant a consent to a candidate.
 *
 * @param consentType type of the consent
 * @param consentVersion version of the consent text
 */
public record GrantConsentRequest(
    @NotBlank @Size(max = 80) String consentType, @Size(max = 40) String consentVersion) {}
