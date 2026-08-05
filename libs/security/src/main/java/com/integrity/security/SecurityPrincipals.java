package com.integrity.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;

/**
 * Helpers to read the platform principal from a Spring Security authentication.
 *
 * <p>Both the raw-user-id form (principal is a {@code String}) and the {@link
 * PlatformAuthenticationToken} form (principal is a {@link PlatformPrincipal}) are supported.
 */
public final class SecurityPrincipals {
  private SecurityPrincipals() {}

  /** Returns the authenticated user id, or {@code null} for anonymous access. */
  public static UUID userId(Authentication authentication) {
    if (authentication == null || !isAuthenticated(authentication)) {
      return null;
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof PlatformPrincipal platformPrincipal) {
      return platformPrincipal.userId();
    }
    if (principal instanceof String subject) {
      try {
        return UUID.fromString(subject);
      } catch (IllegalArgumentException e) {
        return null;
      }
    }
    return null;
  }

  /** Returns the tenant id of the authenticated principal, or {@code null} for anonymous access. */
  public static UUID organizationId(Authentication authentication) {
    if (authentication == null || !isAuthenticated(authentication)) {
      return null;
    }
    if (authentication.getPrincipal() instanceof PlatformPrincipal platformPrincipal) {
      return platformPrincipal.organizationId();
    }
    return null;
  }

  /** Returns the display name of the authenticated principal, or {@code null}. */
  public static String displayName(Authentication authentication) {
    if (authentication == null || !isAuthenticated(authentication)) {
      return null;
    }
    if (authentication.getPrincipal() instanceof PlatformPrincipal platformPrincipal) {
      return platformPrincipal.displayName();
    }
    return null;
  }

  /** Returns true when the principal is authenticated and not anonymous. */
  public static boolean isAuthenticated(Authentication authentication) {
    return authentication != null
        && authentication.isAuthenticated()
        && !"anonymousUser".equals(authentication.getPrincipal());
  }
}
