package com.interviewintegrity.identity.service;

import com.interviewintegrity.exception.AuthenticationFailedException;
import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.identity.domain.PasswordHistory;
import com.interviewintegrity.identity.domain.Role;
import com.interviewintegrity.identity.domain.SessionStatus;
import com.interviewintegrity.identity.domain.User;
import com.interviewintegrity.identity.domain.UserSession;
import com.interviewintegrity.identity.domain.UserStatus;
import com.interviewintegrity.identity.repository.PasswordHistoryRepository;
import com.interviewintegrity.identity.repository.PermissionRepository;
import com.interviewintegrity.identity.repository.RolePermissionRepository;
import com.interviewintegrity.identity.repository.RoleRepository;
import com.interviewintegrity.identity.repository.UserRepository;
import com.interviewintegrity.identity.repository.UserRoleRepository;
import com.interviewintegrity.identity.repository.UserSessionRepository;
import com.interviewintegrity.identity.web.dto.LoginRequest;
import com.interviewintegrity.identity.web.dto.LogoutRequest;
import com.interviewintegrity.identity.web.dto.PasswordResetResponse;
import com.interviewintegrity.identity.web.dto.RefreshRequest;
import com.interviewintegrity.identity.web.dto.RegisterOrganizationRequest;
import com.interviewintegrity.identity.web.dto.RequestPasswordResetRequest;
import com.interviewintegrity.identity.web.dto.ResetPasswordRequest;
import com.interviewintegrity.identity.web.dto.TokenResponse;
import com.interviewintegrity.identity.web.dto.UserResponse;
import com.interviewintegrity.identity.web.dto.VerifyEmailRequest;
import com.interviewintegrity.security.JwtProperties;
import com.interviewintegrity.security.JwtTokenService;
import com.interviewintegrity.security.PlatformPrincipal;
import com.interviewintegrity.security.RefreshTokens;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;

/**
 * Authentication and organization onboarding flows.
 *
 * <p>Owns registration, login, token refresh, logout, email verification and password reset. Issues
 * HMAC signed access tokens via {@link JwtTokenService} and registers opaque refresh tokens in the
 * session table.
 */
public final class AuthService {

  private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);
  private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(15);
  private static final String PURPOSE_RESET = "password-reset";
  private static final String PURPOSE_VERIFY = "email-verify";
  private static final int MULTIPLE_ACCOUNTS = 1;
  private static final int SINGLE_ACCOUNT = 1;

  private final UserRepository userRepository;
  private final UserRoleRepository userRoleRepository;
  private final RoleRepository roleRepository;
  private final RolePermissionRepository rolePermissionRepository;
  private final PermissionRepository permissionRepository;
  private final UserSessionRepository sessionRepository;
  private final PasswordHistoryRepository passwordHistoryRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenService jwtTokenService;
  private final JwtProperties jwtProperties;
  private final AuthorityResolver authorityResolver;
  private final UserEventPublisher eventPublisher;

  /** Creates the auth service with its collaborators. */
  public AuthService(
      UserRepository userRepository,
      UserRoleRepository userRoleRepository,
      RoleRepository roleRepository,
      RolePermissionRepository rolePermissionRepository,
      PermissionRepository permissionRepository,
      UserSessionRepository sessionRepository,
      PasswordHistoryRepository passwordHistoryRepository,
      PasswordEncoder passwordEncoder,
      JwtTokenService jwtTokenService,
      JwtProperties jwtProperties,
      AuthorityResolver authorityResolver,
      UserEventPublisher eventPublisher) {
    this.userRepository = userRepository;
    this.userRoleRepository = userRoleRepository;
    this.roleRepository = roleRepository;
    this.rolePermissionRepository = rolePermissionRepository;
    this.permissionRepository = permissionRepository;
    this.sessionRepository = sessionRepository;
    this.passwordHistoryRepository = passwordHistoryRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenService = jwtTokenService;
    this.jwtProperties = jwtProperties;
    this.authorityResolver = authorityResolver;
    this.eventPublisher = eventPublisher;
  }

  /** Registers a new organization with its first administrator and returns tokens for the admin. */
  public Mono<TokenResponse> register(RegisterOrganizationRequest request) {
    String email = request.adminEmail().toLowerCase(Locale.ROOT);
    return userRepository
        .countLiveByEmail(email)
        .flatMap(
            count -> {
              if (count > 0) {
                return Mono.error(new ConflictException("Email is already registered"));
              }
              return Mono.just(UUID.randomUUID());
            })
        .flatMap(organizationId -> createAdmin(organizationId, request))
        .flatMap(admin -> bootstrapOrgAdminRole(admin).thenReturn(admin))
        .flatMap(
            admin ->
                issueTokens(admin, null, null, null)
                    .flatMap(
                        tokens -> eventPublisher.publishUserRegistered(admin).thenReturn(tokens)));
  }

  private Mono<User> createAdmin(UUID organizationId, RegisterOrganizationRequest request) {
    User admin =
        new User(
            organizationId,
            request.adminEmail().toLowerCase(Locale.ROOT),
            passwordEncoder.encode(request.adminPassword()),
            request.adminDisplayName());
    admin.activate();
    return userRepository.save(admin);
  }

  private Mono<Void> bootstrapOrgAdminRole(User admin) {
    return roleRepository
        .findLiveByOrganizationAndCode(admin.getOrganizationId(), "ORG_ADMIN")
        .switchIfEmpty(
            Mono.defer(
                () ->
                    roleRepository.save(
                        new Role(
                            admin.getOrganizationId(),
                            "ORG_ADMIN",
                            "Organization Administrator",
                            "Full access within the organization",
                            true))))
        .flatMap(
            role ->
                permissionRepository
                    .findAllOrdered()
                    .flatMap(
                        permission ->
                            rolePermissionRepository.grant(
                                role.getId(), permission.getId(), admin.getId()))
                    .then(userRoleRepository.assign(admin.getId(), role.getId(), admin.getId())));
  }

  /** Authenticates a user and issues a token pair. */
  public Mono<TokenResponse> login(LoginRequest request, String ipAddress) {
    String email = request.email().toLowerCase(Locale.ROOT);
    Mono<User> userMono;
    if (request.organizationId() != null) {
      userMono =
          userRepository
              .findLiveByOrganizationAndEmail(request.organizationId(), email)
              .switchIfEmpty(Mono.error(new AuthenticationFailedException("Invalid credentials")));
    } else {
      userMono =
          userRepository
              .findLiveByEmail(email)
              .collectList()
              .flatMap(
                  users -> {
                    if (users.isEmpty()) {
                      return Mono.error(new AuthenticationFailedException("Invalid credentials"));
                    }
                    if (users.size() > MULTIPLE_ACCOUNTS) {
                      return Mono.error(
                          new AuthenticationFailedException(
                              "Email exists in multiple organizations; specify organization id"));
                    }
                    return Mono.just(users.get(0));
                  });
    }
    return userMono.flatMap(
        user ->
            authenticate(
                user, request.password(), request.deviceId(), ipAddress, request.userAgent()));
  }

  private Mono<TokenResponse> authenticate(
      User user, String rawPassword, String deviceId, String ipAddress, String userAgent) {
    if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
      return Mono.error(new AuthenticationFailedException("Invalid credentials"));
    }
    if (user.getStatus() == UserStatus.LOCKED) {
      return Mono.error(new AuthenticationFailedException("Account is locked"));
    }
    if (user.getStatus() == UserStatus.DISABLED) {
      return Mono.error(new AuthenticationFailedException("Account is disabled"));
    }
    if (user.getStatus() != UserStatus.ACTIVE) {
      return Mono.error(new AuthenticationFailedException("Account is not active"));
    }
    user.markLoggedIn();
    return userRepository.save(user).then(issueTokens(user, deviceId, ipAddress, userAgent));
  }

  /** Rotates the refresh token and issues a fresh token pair. */
  public Mono<TokenResponse> refresh(RefreshRequest request) {
    String hash = RefreshTokens.hash(request.refreshToken());
    return sessionRepository
        .findByRefreshTokenHash(hash)
        .switchIfEmpty(Mono.error(new AuthenticationFailedException("Invalid refresh token")))
        .flatMap(
            session -> {
              if (session.getStatus() == SessionStatus.REVOKED
                  || session.getStatus() == SessionStatus.EXPIRED) {
                return Mono.error(
                    new AuthenticationFailedException("Refresh token no longer valid"));
              }
              if (session.getExpiresAt().isBefore(Instant.now())) {
                session.expire();
                return sessionRepository
                    .save(session)
                    .then(Mono.error(new AuthenticationFailedException("Refresh token expired")));
              }
              session.markRefreshed();
              return sessionRepository
                  .save(session)
                  .then(
                      userRepository
                          .findLiveById(session.getUserId())
                          .switchIfEmpty(
                              Mono.error(
                                  new AuthenticationFailedException("User no longer exists")))
                          .flatMap(
                              user ->
                                  issueTokens(
                                      user,
                                      session.getDeviceId(),
                                      session.getIpAddress(),
                                      session.getUserAgent())));
            });
  }

  /** Revokes the session associated with the given refresh token. */
  public Mono<Void> logout(LogoutRequest request) {
    return sessionRepository
        .findByRefreshTokenHash(RefreshTokens.hash(request.refreshToken()))
        .flatMap(
            session -> {
              session.revoke(null);
              return sessionRepository.save(session).then();
            })
        .switchIfEmpty(Mono.empty());
  }

  /** Verifies a user email using a purpose token. */
  public Mono<Void> verifyEmail(VerifyEmailRequest request) {
    return resolveUserByPurposeToken(request.token(), PURPOSE_VERIFY)
        .flatMap(
            user -> {
              user.activate();
              return userRepository.save(user).then();
            });
  }

  /** Requests a password reset and returns the one-time reset token. */
  public Mono<PasswordResetResponse> requestPasswordReset(RequestPasswordResetRequest request) {
    String email = request.email().toLowerCase(Locale.ROOT);
    return userRepository
        .findLiveByEmail(email)
        .collectList()
        .flatMap(
            users -> {
              if (users.size() != SINGLE_ACCOUNT) {
                return Mono.just(new PasswordResetResponse(null, 0L));
              }
              String token =
                  jwtTokenService.issuePurposeToken(
                      PURPOSE_RESET, users.get(0).getId(), RESET_TOKEN_TTL);
              return Mono.just(new PasswordResetResponse(token, RESET_TOKEN_TTL.toSeconds()));
            });
  }

  /** Completes a password reset, records history and revokes all live sessions. */
  public Mono<Void> resetPassword(ResetPasswordRequest request) {
    return resolveUserByPurposeToken(request.token(), PURPOSE_RESET)
        .flatMap(
            user ->
                passwordHistoryRepository
                    .save(new PasswordHistory(user.getId(), user.getPasswordHash(), user.getId()))
                    .then(Mono.fromCallable(() -> passwordEncoder.encode(request.newPassword())))
                    .flatMap(
                        newHash -> {
                          user.changePassword(newHash, user.getId());
                          return userRepository.save(user).then();
                        })
                    .then(sessionRepository.revokeAllActiveByUser(user.getId(), Instant.now()))
                    .then());
  }

  private Mono<User> resolveUserByPurposeToken(String token, String purpose) {
    UUID userId = jwtTokenService.resolvePurposeToken(token, purpose);
    return userRepository
        .findLiveById(userId)
        .switchIfEmpty(Mono.error(new NotFoundException("User no longer exists")));
  }

  private Mono<TokenResponse> issueTokens(
      User user, String deviceId, String ipAddress, String userAgent) {
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
