package com.interviewintegrity.identity.web.dto;

import java.util.List;

/**
 * Enrollment payload for a TOTP MFA device.
 *
 * @param secret base32 encoded TOTP secret to add to an authenticator application
 * @param otpauthUri provisioning URI for QR codes
 * @param recoveryCodes single-use backup codes, shown once and stored only as hashes
 */
public record MfaEnrollResponse(String secret, String otpauthUri, List<String> recoveryCodes) {}
