package com.interviewintegrity.identity.web.dto;

/**
 * Result of a login attempt.
 *
 * <p>Either the caller is authenticated and receives a token pair, or an additional factor is
 * required before tokens can be issued.
 */
public sealed interface LoginResult {

  /** The caller was authenticated and receives a token pair. */
  record Authenticated(TokenResponse tokens) implements LoginResult {}

  /** The caller must satisfy an MFA challenge before tokens are issued. */
  record MfaRequired(MfaChallengeResponse challenge) implements LoginResult {}
}
