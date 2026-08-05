package com.integrity.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to activate a pending TOTP device with a live code.
 *
 * @param code six digit code from the authenticator application
 */
public record MfaTotpVerifyRequest(@NotBlank @Size(min = 6, max = 6) String code) {}
