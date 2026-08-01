package com.interviewintegrity.security;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for shared JWT based authentication.
 *
 * <p>The {@code secret} must be at least 32 bytes when used with HMAC-SHA256. The {@code issuer}
 * and {@code audience} claims are validated on every token. The {@code accessTokenTtl} controls the
 * lifetime of issued access tokens. The {@code permitAll} list contains URL patterns that skip
 * authentication, and {@code cors} lists the allowed browser origins.
 */
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {
  private String secret = "local-development-change-me-32-bytes-minimum";
  private String issuer = "interview-integrity";
  private String audience = "interview-integrity-api";
  private Duration accessTokenTtl = Duration.ofMinutes(15);
  private List<String> permitAll = new ArrayList<>(List.of("/actuator/health", "/actuator/info"));
  private List<String> corsAllowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));

  /** Returns the HMAC signing secret. */
  public String getSecret() {
    return secret;
  }

  /** Sets the HMAC signing secret. */
  public void setSecret(String secret) {
    this.secret = secret;
  }

  /** Returns the expected issuer claim. */
  public String getIssuer() {
    return issuer;
  }

  /** Sets the expected issuer claim. */
  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  /** Returns the expected audience claim. */
  public String getAudience() {
    return audience;
  }

  /** Sets the expected audience claim. */
  public void setAudience(String audience) {
    this.audience = audience;
  }

  /** Returns the access token lifetime. */
  public Duration getAccessTokenTtl() {
    return accessTokenTtl;
  }

  /** Sets the access token lifetime. */
  public void setAccessTokenTtl(Duration accessTokenTtl) {
    this.accessTokenTtl = accessTokenTtl;
  }

  /** Returns the URL patterns that skip authentication. */
  public List<String> getPermitAll() {
    return permitAll;
  }

  /** Sets the URL patterns that skip authentication. */
  public void setPermitAll(List<String> permitAll) {
    this.permitAll = permitAll;
  }

  /** Returns the allowed CORS origins. */
  public List<String> getCorsAllowedOrigins() {
    return corsAllowedOrigins;
  }

  /** Sets the allowed CORS origins. */
  public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
    this.corsAllowedOrigins = corsAllowedOrigins;
  }
}
