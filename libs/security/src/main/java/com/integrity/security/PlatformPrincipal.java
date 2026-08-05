package com.integrity.security;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * Authenticated platform principal extracted from a validated access token.
 *
 * @param userId unique user identifier, also the JWT subject
 * @param organizationId tenant the user belongs to
 * @param email user email address
 * @param displayName human readable display name
 * @param authorities granted authority strings, e.g. {@code ROLE_RECRUITER}
 */
public record PlatformPrincipal(
    UUID userId, UUID organizationId, String email, String displayName, List<String> authorities)
    implements Serializable {

  private static final long serialVersionUID = 1L;

  /** Compact constructor defensively copies the authority list. */
  public PlatformPrincipal {
    authorities = List.copyOf(authorities);
  }
}
