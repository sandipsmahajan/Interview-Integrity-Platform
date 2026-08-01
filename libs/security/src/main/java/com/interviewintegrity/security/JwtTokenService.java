package com.interviewintegrity.security;

import java.time.Duration;
import java.util.UUID;

/**
 * Issues and validates the platform's HMAC signed JWT access tokens.
 *
 * <p>The token is symmetric: every service shares the signing secret through {@link JwtProperties}
 * so that any service can verify tokens issued by the identity service.
 */
public interface JwtTokenService {

  /**
   * Creates a signed access token for the given principal.
   *
   * @param principal the authenticated principal to embed in the token
   * @return compact serialized JWT
   */
  String issueAccessToken(PlatformPrincipal principal);

  /**
   * Validates a compact JWT and extracts the embedded principal.
   *
   * @param token compact serialized JWT
   * @return the principal embedded in the token
   * @throws com.interviewintegrity.exception.AuthenticationFailedException when the token is
   *     malformed, expired, or fails signature/issuer/audience validation
   */
  PlatformPrincipal parseAccessToken(String token);

  /**
   * Issues a short-lived purpose bound token used for one-time flows such as email verification or
   * password reset.
   *
   * @param purpose stable purpose discriminator embedded as a claim
   * @param subject identifier of the target entity, normally a user id
   * @param ttl lifetime of the purpose token
   * @return compact serialized JWT
   */
  String issuePurposeToken(String purpose, UUID subject, Duration ttl);

  /**
   * Validates a purpose token and returns its subject when the purpose matches.
   *
   * @param token compact serialized JWT
   * @param purpose expected purpose claim
   * @return the subject embedded in the token
   * @throws com.interviewintegrity.exception.AuthenticationFailedException when the token is
   *     invalid, expired, or carries a different purpose
   */
  UUID resolvePurposeToken(String token, String purpose);
}
