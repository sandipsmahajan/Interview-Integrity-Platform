package com.integrity.identity.config;

import java.time.Duration;

/**
 * Configuration for email and MFA related authentication flows.
 *
 * @param appName application name used in email greeting and branding
 * @param frontendBaseUrl base URL of the web client used to build verification and reset links
 * @param mfaChallengeTtl lifetime of an MFA login challenge token
 * @param mfaEmailPurpose purpose discriminator used for login email OTP codes
 * @param exposeResetToken whether the password reset API response includes the one-time token
 * @param resetRequestInterval minimum interval between password reset requests per email
 * @param maxLoginAttempts consecutive failed password attempts before the account is locked
 * @param loginLockout duration of the temporary account lockout after repeated failures
 * @param maxMfaChallengeAttempts failed MFA factor verifications before a challenge is invalidated
 */
public record AuthProperties(
    String appName,
    String frontendBaseUrl,
    Duration mfaChallengeTtl,
    String mfaEmailPurpose,
    boolean exposeResetToken,
    Duration resetRequestInterval,
    int maxLoginAttempts,
    Duration loginLockout,
    int maxMfaChallengeAttempts) {

  /** Creates the properties with defaults. */
  public AuthProperties {
    if (appName == null || appName.isBlank()) {
      appName = "Integrity Pro";
    }
    if (frontendBaseUrl == null || frontendBaseUrl.isBlank()) {
      frontendBaseUrl = "http://localhost:5173";
    }
    if (mfaChallengeTtl == null) {
      mfaChallengeTtl = Duration.ofMinutes(5);
    }
    if (mfaEmailPurpose == null || mfaEmailPurpose.isBlank()) {
      mfaEmailPurpose = "mfa-login";
    }
    if (resetRequestInterval == null) {
      resetRequestInterval = Duration.ofMinutes(1);
    }
    if (maxLoginAttempts <= 0) {
      maxLoginAttempts = 5;
    }
    if (loginLockout == null || loginLockout.isZero() || loginLockout.isNegative()) {
      loginLockout = Duration.ofMinutes(15);
    }
    if (maxMfaChallengeAttempts <= 0) {
      maxMfaChallengeAttempts = 5;
    }
  }
}
