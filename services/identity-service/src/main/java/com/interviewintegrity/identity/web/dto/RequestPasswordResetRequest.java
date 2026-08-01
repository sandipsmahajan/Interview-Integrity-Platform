package com.interviewintegrity.identity.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to start a password reset for an email address.
 *
 * @param email registered email address
 */
public record RequestPasswordResetRequest(@NotBlank @Email @Size(max = 320) String email) {}
