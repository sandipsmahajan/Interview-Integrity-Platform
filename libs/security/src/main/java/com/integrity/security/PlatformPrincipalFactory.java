package com.integrity.security;

import com.integrity.exception.AuthenticationFailedException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Maps validated JWTs onto {@link PlatformPrincipal} instances using the platform claim contract.
 */
public final class PlatformPrincipalFactory {

  private PlatformPrincipalFactory() {}

  /**
   * Builds a {@link PlatformPrincipal} from a validated access token.
   *
   * @throws AuthenticationFailedException when required claims are absent or malformed
   */
  public static PlatformPrincipal from(Jwt jwt) {
    String subject = jwt.getSubject();
    String organization = jwt.getClaimAsString(HmacJwtTokenService.CLAIM_ORGANIZATION);
    if (subject == null || organization == null) {
      throw new AuthenticationFailedException("Access token is missing required claims");
    }
    try {
      List<String> authorities = jwt.getClaimAsStringList(HmacJwtTokenService.CLAIM_AUTHORITIES);
      return new PlatformPrincipal(
          UUID.fromString(subject),
          UUID.fromString(organization),
          jwt.getClaimAsString(HmacJwtTokenService.CLAIM_EMAIL),
          jwt.getClaimAsString(HmacJwtTokenService.CLAIM_DISPLAY_NAME),
          authorities);
    } catch (IllegalArgumentException e) {
      throw new AuthenticationFailedException("Access token contains invalid identifiers", e);
    }
  }
}
