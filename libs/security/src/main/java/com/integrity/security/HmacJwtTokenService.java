package com.integrity.security;

import com.integrity.exception.AuthenticationFailedException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * HMAC-SHA256 JWT implementation of {@link JwtTokenService}.
 *
 * <p>Uses the shared secret from {@link JwtProperties} both for signing (identity service) and for
 * verifying (every other service).
 */
public final class HmacJwtTokenService implements JwtTokenService {

  static final String CLAIM_ORGANIZATION = "organization_id";
  static final String CLAIM_EMAIL = "email";
  static final String CLAIM_DISPLAY_NAME = "display_name";
  static final String CLAIM_AUTHORITIES = "authorities";
  static final String CLAIM_PURPOSE = "purpose";
  static final String KEY_ID = "interview-integrity-hs256";

  private final JwtProperties properties;
  private final SecretKey secretKey;
  private final NimbusJwtDecoder decoder;

  /** Creates a service bound to the given properties. */
  public HmacJwtTokenService(JwtProperties properties) {
    this.properties = properties;
    SecretKeys.validate(properties.getSecret());
    this.secretKey =
        new SecretKeySpec(properties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    NimbusJwtDecoder newDecoder = NimbusJwtDecoder.withSecretKey(secretKey).build();
    newDecoder.setJwtValidator(SecretKeys.defaultValidator(properties));
    this.decoder = newDecoder;
  }

  @Override
  public String issueAccessToken(PlatformPrincipal principal) {
    Instant now = Instant.now();
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject(principal.userId().toString())
            .issuer(properties.getIssuer())
            .audience(List.of(properties.getAudience()))
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(properties.getAccessTokenTtl())))
            .jwtID(UUID.randomUUID().toString())
            .claim(CLAIM_ORGANIZATION, principal.organizationId().toString())
            .claim(CLAIM_EMAIL, principal.email())
            .claim(CLAIM_DISPLAY_NAME, principal.displayName())
            .claim(CLAIM_AUTHORITIES, principal.authorities())
            .build();
    try {
      SignedJWT signedJwt =
          new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).keyID(KEY_ID).build(), claims);
      signedJwt.sign(new MACSigner(secretKey));
      return signedJwt.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException("Unable to sign access token", e);
    }
  }

  @Override
  public PlatformPrincipal parseAccessToken(String token) {
    if (token == null || token.isBlank()) {
      throw new AuthenticationFailedException("Missing access token");
    }
    try {
      Jwt jwt = decoder.decode(token);
      return PlatformPrincipalFactory.from(jwt);
    } catch (AuthenticationFailedException e) {
      throw e;
    } catch (org.springframework.security.oauth2.jwt.JwtException e) {
      throw new AuthenticationFailedException("Invalid or expired access token", e);
    }
  }

  @Override
  public UUID resolvePurposeToken(String token, String purpose) {
    if (token == null || token.isBlank()) {
      throw new AuthenticationFailedException("Missing token");
    }
    try {
      Jwt jwt = decoder.decode(token);
      if (!purpose.equals(jwt.getClaimAsString(CLAIM_PURPOSE))) {
        throw new AuthenticationFailedException("Invalid token purpose");
      }
      if (jwt.getSubject() == null) {
        throw new AuthenticationFailedException("Token is missing a subject");
      }
      return UUID.fromString(jwt.getSubject());
    } catch (AuthenticationFailedException e) {
      throw e;
    } catch (org.springframework.security.oauth2.jwt.JwtException | IllegalArgumentException e) {
      throw new AuthenticationFailedException("Invalid or expired token", e);
    }
  }

  @Override
  public String issuePurposeToken(String purpose, UUID subject, Duration ttl) {
    Instant now = Instant.now();
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject(subject.toString())
            .issuer(properties.getIssuer())
            .audience(List.of(properties.getAudience()))
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(ttl)))
            .jwtID(UUID.randomUUID().toString())
            .claim(CLAIM_PURPOSE, purpose)
            .build();
    try {
      SignedJWT signedJwt =
          new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).keyID(KEY_ID).build(), claims);
      signedJwt.sign(new MACSigner(secretKey));
      return signedJwt.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException("Unable to sign purpose token", e);
    }
  }
}
