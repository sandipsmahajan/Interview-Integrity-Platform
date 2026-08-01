package com.interviewintegrity.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to complete a password reset.
 *
 * @param token one-time reset token
 * @param newPassword replacement password
 */
public record ResetPasswordRequest(
    @NotBlank String token, @NotBlank @Size(min = 8, max = 128) String newPassword) {}
