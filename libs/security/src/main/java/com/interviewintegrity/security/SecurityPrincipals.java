package com.interviewintegrity.security;

import org.springframework.security.core.Authentication;

/** Helpers to read the platform principal from a Spring Security authentication. */
public final class SecurityPrincipals {
  private SecurityPrincipals() {}

  /** Returns the authenticated user id, or {@code null} for anonymous access. */
  public static String userId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof String username) {
      return username;
    }
    return authentication.getName();
  }

  /** Returns true when the principal is authenticated and not anonymous. */
  public static boolean isAuthenticated(Authentication authentication) {
    return authentication != null
        && authentication.isAuthenticated()
        && !"anonymousUser".equals(authentication.getPrincipal());
  }
}
