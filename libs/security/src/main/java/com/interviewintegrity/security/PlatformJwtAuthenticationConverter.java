package com.interviewintegrity.security;

import com.interviewintegrity.exception.AuthenticationFailedException;
import java.util.List;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

/**
 * Converts a validated {@link Jwt} into a {@link PlatformAuthenticationToken} whose principal is a
 * {@link PlatformPrincipal}. Used by resource servers to build the reactive security context.
 */
public final class PlatformJwtAuthenticationConverter
    implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

  @Override
  public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
    return Mono.just(jwt)
        .map(PlatformJwtAuthenticationConverter::toPrincipal)
        .map(PlatformAuthenticationToken::new);
  }

  private static PlatformPrincipal toPrincipal(Jwt jwt) {
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
