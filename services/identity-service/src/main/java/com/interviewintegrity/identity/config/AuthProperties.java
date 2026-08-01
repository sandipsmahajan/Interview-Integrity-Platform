package com.interviewintegrity.identity.config;

import java.time.Duration;

/**
 * Configuration for email and MFA related authentication flows.
 *
 * @param appName application name used in email greeting and branding
 * @param frontendBaseUrl base URL of the web client used to build verification and reset links
 * @param mfaChallengeTtl lifetime of an MFA login challenge token
 * @param mfaEmailPurpose purpose discriminator used for login email OTP codes
 */
public record AuthProperties(
    String appName, String frontendBaseUrl, Duration mfaChallengeTtl, String mfaEmailPurpose) {

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
  }
}
