package com.integrity.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Authenticated principal that carries the {@link PlatformPrincipal} extracted from a validated
 * access token.
 */
public final class PlatformAuthenticationToken extends AbstractAuthenticationToken {

  private static final long serialVersionUID = 1L;

  private final PlatformPrincipal principal;

  /** Wraps the given principal with its granted authorities. */
  public PlatformAuthenticationToken(PlatformPrincipal principal) {
    super(toAuthorities(principal.authorities()));
    this.principal = principal;
    setAuthenticated(true);
  }

  private static Collection<? extends GrantedAuthority> toAuthorities(List<String> authorities) {
    return authorities.stream().map(SimpleGrantedAuthority::new).toList();
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return principal;
  }
}
