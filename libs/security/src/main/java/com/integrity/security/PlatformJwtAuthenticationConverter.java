package com.integrity.security;

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
    return Mono.just(jwt).map(PlatformPrincipalFactory::from).map(PlatformAuthenticationToken::new);
  }
}
