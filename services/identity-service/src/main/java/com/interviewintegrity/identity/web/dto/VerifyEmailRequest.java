package com.interviewintegrity.identity.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to verify a user email with a one-time token.
 *
 * @param token one-time verification token
 */
public record VerifyEmailRequest(@NotBlank String token) {}
