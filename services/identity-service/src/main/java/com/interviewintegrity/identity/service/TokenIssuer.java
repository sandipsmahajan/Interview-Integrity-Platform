package com.interviewintegrity.identity.service;

import com.interviewintegrity.identity.domain.User;
import com.interviewintegrity.identity.domain.UserSession;
import com.interviewintegrity.identity.repository.UserSessionRepository;
import com.interviewintegrity.identity.web.dto.TokenResponse;
import com.interviewintegrity.identity.web.dto.UserResponse;
import com.interviewintegrity.security.JwtProperties;
import com.interviewintegrity.security.JwtTokenService;
import com.interviewintegrity.security.PlatformPrincipal;
import com.interviewintegrity.security.RefreshTokens;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Issues access and refresh token pairs and registers the refresh token session.
 *
 * <p>Shared by the authentication and MFA flows so both grant sessions in exactly the same way.
 */
public final class TokenIssuer {

  private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

  private final AuthorityResolver authorityResolver;
  private final JwtTokenService jwtTokenService;
  private final JwtProperties jwtProperties;
  private final UserSessionRepository sessionRepository;

  /** Creates the token issuer with its collaborators. */
  public TokenIssuer(
      AuthorityResolver authorityResolver,
      JwtTokenService jwtTokenService,
      JwtProperties jwtProperties,
      UserSessionRepository sessionRepository) {
    this.authorityResolver = authorityResolver;
    this.jwtTokenService = jwtTokenService;
    this.jwtProperties = jwtProperties;
    this.sessionRepository = sessionRepository;
  }

  /** Issues a fresh token pair for the user and registers the refresh token session. */
  public Mono<TokenResponse> issue(User user, String deviceId, String ipAddress, String userAgent) {
    return authorityResolver
        .resolve(user.getId())
        .flatMap(
            authorities -> {
              PlatformPrincipal principal =
                  new PlatformPrincipal(
                      user.getId(),
                      user.getOrganizationId(),
                      user.getEmail(),
                      user.getDisplayName(),
                      authorities);
              String accessToken = jwtTokenService.issueAccessToken(principal);
              String refreshToken = RefreshTokens.generate();
              UserSession session =
                  new UserSession(
                      user.getId(),
                      user.getOrganizationId(),
                      RefreshTokens.hash(refreshToken),
                      deviceId,
                      ipAddress,
                      userAgent,
                      Instant.now().plus(REFRESH_TOKEN_TTL));
              return sessionRepository
                  .save(session)
                  .map(
                      saved ->
                          new TokenResponse(
                              accessToken,
                              refreshToken,
                              jwtProperties.getAccessTokenTtl().toSeconds(),
                              "Bearer",
                              toUserResponse(user, roleCodes(authorities))));
            });
  }

  private List<String> roleCodes(List<String> authorities) {
    return authorities.stream()
        .filter(authority -> authority.startsWith("ROLE_"))
        .map(authority -> authority.substring("ROLE_".length()))
        .toList();
  }

  private UserResponse toUserResponse(User user, List<String> roleCodes) {
    return new UserResponse(
        user.getId(),
        user.getOrganizationId(),
        user.getEmail(),
        user.getDisplayName(),
        user.getStatus().name(),
        user.getEmailVerifiedAt(),
        user.getLastLoginAt(),
        user.getCreatedAt(),
        roleCodes);
  }
}
