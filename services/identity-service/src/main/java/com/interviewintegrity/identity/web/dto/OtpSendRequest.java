package com.interviewintegrity.identity.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to deliver a one-time passcode by email.
 *
 * @param email recipient email address
 * @param purpose stable purpose discriminator for the code, e.g. {@code mfa-login}
 */
public record OtpSendRequest(
    @NotBlank @Email @Size(max = 320) String email, @NotBlank @Size(max = 64) String purpose) {}
