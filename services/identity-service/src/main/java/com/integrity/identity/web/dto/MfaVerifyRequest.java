package com.integrity.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to complete an MFA login challenge.
 *
 * @param challengeId challenge token returned by a login that required a second factor
 * @param code the code supplied by the user (TOTP, email OTP or recovery code)
 * @param trustDevice when true the device is exempted from future MFA challenges
 * @param deviceId stable device identifier, required when trusting the device
 * @param deviceName human readable device label, e.g. {@code MacBook Pro}
 */
public record MfaVerifyRequest(
    @NotBlank String challengeId,
    @NotBlank @Size(max = 32) String code,
    boolean trustDevice,
    @Size(max = 200) String deviceId,
    @Size(max = 200) String deviceName) {}
