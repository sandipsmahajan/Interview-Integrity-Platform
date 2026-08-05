package com.integrity.security;

import java.util.List;

/**
 * Public URL patterns that must skip authentication at the gateway and identity service.
 *
 * <p>These paths implement the pre-authentication surface of the platform: registering, signing in,
 * refreshing tokens and completing password or MFA recovery. They are listed here so the gateway
 * can forward them without a token and identity can accept them without one, keeping the two
 * configurations from drifting apart.
 */
public final class PublicAuthPaths {

  public static final List<String> PATHS =
      List.of(
          "/api/v1/auth/register",
          "/api/v1/auth/login",
          "/api/v1/auth/refresh",
          "/api/v1/auth/logout",
          "/api/v1/auth/verify-email",
          "/api/v1/auth/password/reset-request",
          "/api/v1/auth/password/reset",
          "/api/v1/auth/otp/send",
          "/api/v1/auth/otp/verify",
          "/api/v1/auth/mfa/verify",
          "/api/v1/auth/mfa/email-otp");

  private PublicAuthPaths() {}
}
