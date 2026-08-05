package com.integrity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.integrity.exception.AuthenticationFailedException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link HmacJwtTokenService} access and purpose token round trips. */
class HmacJwtTokenServiceTest {

  private static final String SECRET = "test-secret-at-least-32-bytes-long-value";
  private static final String ISSUER = "interview-integrity";
  private static final String AUDIENCE = "interview-integrity-api";
  private static final String EMAIL = "alice@example.com";
  private static final String RESET_PURPOSE = "password-reset";

  private final JwtProperties jwtProperties = properties();

  private static JwtProperties properties() {
    JwtProperties props = new JwtProperties();
    props.setSecret(SECRET);
    props.setIssuer(ISSUER);
    props.setAudience(AUDIENCE);
    props.setAccessTokenTtl(Duration.ofMinutes(15));
    return props;
  }

  private static PlatformPrincipal principal() {
    return new PlatformPrincipal(
        UUID.randomUUID(),
        UUID.randomUUID(),
        EMAIL,
        "Alice",
        List.of("ROLE_ORG_ADMIN", "user:read"));
  }

  @Test
  void accessTokenRoundTripPreservesPrincipalClaims() {
    JwtTokenService service = new HmacJwtTokenService(jwtProperties);
    UUID userId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    PlatformPrincipal principal =
        new PlatformPrincipal(
            userId, organizationId, EMAIL, "Alice", List.of("ROLE_ORG_ADMIN", "user:read"));

    PlatformPrincipal parsed = service.parseAccessToken(service.issueAccessToken(principal));

    assertThat(parsed)
        .extracting(PlatformPrincipal::userId, PlatformPrincipal::organizationId)
        .containsExactly(userId, organizationId);
    assertThat(parsed)
        .extracting(PlatformPrincipal::email, PlatformPrincipal::displayName)
        .containsExactly(EMAIL, "Alice");
    assertThat(parsed.authorities()).containsExactly("ROLE_ORG_ADMIN", "user:read");
  }

  @Test
  void parseAccessTokenRejectsTokenSignedWithDifferentKey() {
    JwtTokenService service = new HmacJwtTokenService(jwtProperties);
    JwtProperties otherProperties = properties();
    otherProperties.setSecret("another-secret-value-of-at-least-32-bytes");
    JwtTokenService other = new HmacJwtTokenService(otherProperties);

    String token = other.issueAccessToken(principal());

    assertThatThrownBy(() -> service.parseAccessToken(token))
        .isInstanceOf(AuthenticationFailedException.class);
  }

  @Test
  void parseAccessTokenRejectsTokenFromDifferentIssuer() {
    JwtProperties wrongIssuerProperties = properties();
    wrongIssuerProperties.setIssuer("some-other-platform");
    JwtTokenService wrongIssuer = new HmacJwtTokenService(wrongIssuerProperties);

    String token = wrongIssuer.issueAccessToken(principal());

    assertThatThrownBy(() -> new HmacJwtTokenService(jwtProperties).parseAccessToken(token))
        .isInstanceOf(AuthenticationFailedException.class);
  }

  @Test
  void parseAccessTokenRejectsMalformedToken() {
    JwtTokenService service = new HmacJwtTokenService(jwtProperties);
    assertThatThrownBy(() -> service.parseAccessToken("not-a-jwt"))
        .isInstanceOf(AuthenticationFailedException.class);
  }

  @Test
  void parseAccessTokenRejectsMissingToken() {
    JwtTokenService service = new HmacJwtTokenService(jwtProperties);
    assertThatThrownBy(() -> service.parseAccessToken(null))
        .isInstanceOf(AuthenticationFailedException.class);
    assertThatThrownBy(() -> service.parseAccessToken("  "))
        .isInstanceOf(AuthenticationFailedException.class);
  }

  @Test
  void purposeTokenRoundTripResolvesSubject() {
    JwtTokenService service = new HmacJwtTokenService(jwtProperties);
    UUID userId = UUID.randomUUID();

    String token = service.issuePurposeToken(RESET_PURPOSE, userId, Duration.ofMinutes(15));
    UUID resolved = service.resolvePurposeToken(token, RESET_PURPOSE);

    assertThat(resolved).isEqualTo(userId);
  }

  @Test
  void resolvePurposeTokenRejectsMismatchedPurpose() {
    JwtTokenService service = new HmacJwtTokenService(jwtProperties);
    String token =
        service.issuePurposeToken(RESET_PURPOSE, UUID.randomUUID(), Duration.ofMinutes(15));

    assertThatThrownBy(() -> service.resolvePurposeToken(token, "email-verify"))
        .isInstanceOf(AuthenticationFailedException.class);
  }

  @Test
  void resolvePurposeTokenRejectsAccessToken() {
    JwtTokenService service = new HmacJwtTokenService(jwtProperties);
    String accessToken = service.issueAccessToken(principal());

    assertThatThrownBy(() -> service.resolvePurposeToken(accessToken, RESET_PURPOSE))
        .isInstanceOf(AuthenticationFailedException.class);
  }

  @Test
  void expiredAccessTokenIsRejected() {
    JwtProperties shortLived = properties();
    shortLived.setAccessTokenTtl(Duration.ofMillis(1));
    JwtTokenService service = new HmacJwtTokenService(shortLived);

    String token = service.issueAccessToken(principal());
    try {
      Thread.sleep(50);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }

    assertThatThrownBy(() -> service.parseAccessToken(token))
        .isInstanceOf(AuthenticationFailedException.class);
  }
}
