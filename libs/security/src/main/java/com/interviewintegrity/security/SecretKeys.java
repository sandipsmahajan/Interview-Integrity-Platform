package com.interviewintegrity.security;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;

/**
 * Validates the shared HMAC signing secret and builds token validators.
 *
 * <p>The platform signs and verifies access tokens with a single shared secret. A weak or default
 * secret would let any caller forge tokens for any tenant, so startup fails fast when the secret is
 * shorter than the HMAC-SHA256 minimum. The documented development fallback is rejected outside
 * local and test profiles so it can never be deployed.
 */
public final class SecretKeys {

  /** Development fallback that must never reach a non-local environment. */
  static final String DEVELOPMENT_DEFAULT = "local-development-change-me-32-bytes-minimum";

  private static final int MIN_BYTES = 32;

  private SecretKeys() {}

  /**
   * Ensures the configured secret is strong enough for HMAC-SHA256.
   *
   * @throws IllegalStateException when the secret is missing or shorter than the HMAC minimum
   */
  public static void validate(String secret) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException(
          "security.jwt.secret must be configured with at least " + MIN_BYTES + " bytes");
    }
    if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_BYTES) {
      throw new IllegalStateException(
          "security.jwt.secret must be at least " + MIN_BYTES + " bytes for HMAC-SHA256");
    }
  }

  /**
   * Applies {@link #validate(String)} and rejects the development fallback outside local/test.
   *
   * @throws IllegalStateException when the secret is weak or is the development default in a
   *     non-local profile
   */
  public static void validateForDeployment(String secret, Environment environment) {
    validate(secret);
    if (DEVELOPMENT_DEFAULT.equals(secret) && hasExplicitNonLocalProfile(environment)) {
      throw new IllegalStateException(
          "security.jwt.secret must not use the development default in a deployed environment");
    }
  }

  private static boolean hasExplicitNonLocalProfile(Environment environment) {
    String[] profiles = environment.getActiveProfiles();
    if (profiles.length == 0) {
      return false;
    }
    for (String profile : profiles) {
      if (!"local".equals(profile) && !"test".equals(profile)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Builds the default validator enforcing timestamp, issuer and audience claims.
   *
   * <p>Both the token service and the resource-server decoder use this so validation never drifts
   * between the issuing and consuming paths.
   */
  public static OAuth2TokenValidator<Jwt> defaultValidator(JwtProperties properties) {
    String audience = properties.getAudience();
    OAuth2TokenValidator<Jwt> audienceValidator =
        new JwtClaimValidator<List<String>>("aud", aud -> aud != null && aud.contains(audience));
    return new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefaultWithIssuer(properties.getIssuer()), audienceValidator);
  }
}
