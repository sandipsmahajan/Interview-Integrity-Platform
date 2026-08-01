package com.interviewintegrity.identity.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to deliver an email OTP for a pending MFA challenge.
 *
 * @param challengeId challenge token returned by a login that required a second factor
 */
public record MfaEmailOtpRequest(@NotBlank String challengeId) {}
