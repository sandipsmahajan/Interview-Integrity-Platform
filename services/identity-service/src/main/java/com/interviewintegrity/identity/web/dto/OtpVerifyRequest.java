package com.interviewintegrity.identity.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to verify a one-time passcode delivered by email.
 *
 * @param email recipient email address
 * @param purpose stable purpose discriminator for the code
 * @param code the six digit code supplied by the user
 */
public record OtpVerifyRequest(
    @NotBlank @Email @Size(max = 320) String email,
    @NotBlank @Size(max = 64) String purpose,
    @NotBlank @Size(min = 6, max = 6) String code) {}
