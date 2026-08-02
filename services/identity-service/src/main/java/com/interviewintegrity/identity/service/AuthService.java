package com.interviewintegrity.identity.service;

import com.interviewintegrity.event.IdentityEmailEvent;
import com.interviewintegrity.exception.AuthenticationFailedException;
import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.identity.config.AuthProperties;
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
import com.interviewintegrity.identity.web.dto.LoginResult;
import com.interviewintegrity.identity.web.dto.LogoutRequest;
import com.interviewintegrity.identity.web.dto.PasswordResetResponse;
import com.interviewintegrity.identity.web.dto.RefreshRequest;
import com.interviewintegrity.identity.web.dto.RegisterOrganizationRequest;
import com.interviewintegrity.identity.web.dto.RequestPasswordResetRequest;
import com.interviewintegrity.identity.web.dto.ResetPasswordRequest;
import com.interviewintegrity.identity.web.dto.TokenResponse;
import com.interviewintegrity.identity.web.dto.VerifyEmailRequest;
import com.interviewintegrity.security.JwtTokenService;
import com.interviewintegrity.security.RefreshTokens;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Authentication and organization onboarding flows.
 *
 * <p>Owns registration, login, token refresh, logout, email verification and password reset. Login
 * returns a token pair when the account has no MFA or uses a trusted device, otherwise a challenge
 * that must be satisfied through {@link MfaService}. Email delivery is requested through the
 * platform event bus via {@link EmailEventPublisher}.
 */
public final class AuthService {

  private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(15);
  private static final String PURPOSE_RESET = "password-reset";
  private static final String PURPOSE_VERIFY = "email-verify";
  private static final String DEFAULT_LOCALE = "en";
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
  private final UserEventPublisher eventPublisher;
  private final EmailEventPublisher emailEventPublisher;
  private final TokenIssuer tokenIssuer;
  private final MfaService mfaService;
  private final AuthProperties authProperties;

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
      UserEventPublisher eventPublisher,
      EmailEventPublisher emailEventPublisher,
      TokenIssuer tokenIssuer,
      MfaService mfaService,
      AuthProperties authProperties) {
    this.userRepository = userRepository;
    this.userRoleRepository = userRoleRepository;
    this.roleRepository = roleRepository;
    this.rolePermissionRepository = rolePermissionRepository;
    this.permissionRepository = permissionRepository;
    this.sessionRepository = sessionRepository;
    this.passwordHistoryRepository = passwordHistoryRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenService = jwtTokenService;
    this.eventPublisher = eventPublisher;
    this.emailEventPublisher = emailEventPublisher;
    this.tokenIssuer = tokenIssuer;
    this.mfaService = mfaService;
    this.authProperties = authProperties;
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
                tokenIssuer
                    .issue(admin, null, null, null)
                    .flatMap(
                        tokens ->
                            eventPublisher
                                .publishUserRegistered(admin)
                                .then(publishWelcomeEmails(admin))
                                .thenReturn(tokens)));
  }

  private Mono<User> createAdmin(UUID organizationId, RegisterOrganizationRequest request) {
    return Mono.fromCallable(() -> passwordEncoder.encode(request.adminPassword()))
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(
            encoded -> {
              User admin =
                  new User(
                      organizationId,
                      request.adminEmail().toLowerCase(Locale.ROOT),
                      encoded,
                      request.adminDisplayName());
              admin.activate();
              return userRepository.save(admin);
            });
  }

  private Mono<Void> publishWelcomeEmails(User user) {
    String verifyToken =
        jwtTokenService.issuePurposeToken(PURPOSE_VERIFY, user.getId(), RESET_TOKEN_TTL);
    String verifyUrl = authProperties.frontendBaseUrl() + "/verify-email?token=" + verifyToken;
    Instant now = Instant.now();
    return emailEventPublisher
        .publish(
            new IdentityEmailEvent(
                user.getId(),
                user.getOrganizationId(),
                user.getEmail(),
                user.getDisplayName(),
                DEFAULT_LOCALE,
                "email-verification",
                Map.of(
                    "verificationUrl",
                    verifyUrl,
                    "expiresInMinutes",
                    String.valueOf(RESET_TOKEN_TTL.toMinutes())),
                now))
        .then(
            emailEventPublisher.publish(
                new IdentityEmailEvent(
                    user.getId(),
                    user.getOrganizationId(),
                    user.getEmail(),
                    user.getDisplayName(),
                    DEFAULT_LOCALE,
                    "welcome",
                    Map.of("appName", authProperties.appName()),
                    now)));
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

  /** Authenticates a user and returns tokens or an MFA challenge. */
  public Mono<LoginResult> login(LoginRequest request, String ipAddress) {
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

  private Mono<LoginResult> authenticate(
      User user, String rawPassword, String deviceId, String ipAddress, String userAgent) {
    if (user.getStatus() == UserStatus.LOCKED) {
      return Mono.error(new AuthenticationFailedException("Account is locked"));
    }
    if (user.isLockedOut()) {
      return Mono.error(new AuthenticationFailedException("Account is temporarily locked"));
    }
    if (user.getStatus() == UserStatus.DISABLED) {
      return Mono.error(new AuthenticationFailedException("Account is disabled"));
    }
    if (user.getStatus() != UserStatus.ACTIVE) {
      return Mono.error(new AuthenticationFailedException("Account is not active"));
    }
    return Mono.fromCallable(() -> passwordEncoder.matches(rawPassword, user.getPasswordHash()))
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(
            matches -> {
              if (!matches) {
                return recordFailedLogin(user)
                    .then(Mono.error(new AuthenticationFailedException("Invalid credentials")));
              }
              return mfaService
                  .hasVerifiedDevice(user.getId())
                  .flatMap(
                      hasMfa -> {
                        if (!hasMfa) {
                          return grantTokens(user, deviceId, ipAddress, userAgent);
                        }
                        return mfaService
                            .isTrustedDevice(user.getId(), deviceId)
                            .flatMap(
                                trusted ->
                                    trusted
                                        ? grantTokens(user, deviceId, ipAddress, userAgent)
                                        : mfaService
                                            .generateChallenge(user)
                                            .map(LoginResult.MfaRequired::new));
                      });
            });
  }

  private Mono<Void> recordFailedLogin(User user) {
    user.recordFailedLogin(authProperties.maxLoginAttempts(), authProperties.loginLockout());
    return userRepository.save(user).then();
  }

  private Mono<LoginResult> grantTokens(
      User user, String deviceId, String ipAddress, String userAgent) {
    user.markLoggedIn();
    return userRepository
        .save(user)
        .then(tokenIssuer.issue(user, deviceId, ipAddress, userAgent))
        .map(LoginResult.Authenticated::new);
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
                return onRefreshTokenReuse(session)
                    .then(
                        Mono.error(
                            new AuthenticationFailedException("Refresh token no longer valid")));
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
                                  tokenIssuer.issue(
                                      user,
                                      session.getDeviceId(),
                                      session.getIpAddress(),
                                      session.getUserAgent())));
            });
  }

  /**
   * Detects refresh token reuse.
   *
   * <p>Presenting an already revoked or rotated token indicates the token was stolen, so every live
   * session of the user is revoked to evict the attacker before the error is surfaced.
   */
  private Mono<Void> onRefreshTokenReuse(UserSession session) {
    return sessionRepository.revokeAllActiveByUser(session.getUserId(), Instant.now()).then();
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

  /** Requests a password reset and returns the one-time reset token, delivered by email. */
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
              User user = users.get(0);
              if (user.recentlyRequestedReset(authProperties.resetRequestInterval())) {
                return Mono.just(new PasswordResetResponse(null, 0L));
              }
              String token =
                  jwtTokenService.issuePurposeToken(PURPOSE_RESET, user.getId(), RESET_TOKEN_TTL);
              String resetUrl = authProperties.frontendBaseUrl() + "/reset-password?token=" + token;
              user.recordPasswordResetRequest();
              return userRepository
                  .save(user)
                  .then(
                      emailEventPublisher.publish(
                          new IdentityEmailEvent(
                              user.getId(),
                              user.getOrganizationId(),
                              user.getEmail(),
                              user.getDisplayName(),
                              DEFAULT_LOCALE,
                              "password-reset",
                              Map.of(
                                  "resetUrl",
                                  resetUrl,
                                  "expiresInMinutes",
                                  String.valueOf(RESET_TOKEN_TTL.toMinutes())),
                              Instant.now())))
                  .thenReturn(
                      authProperties.exposeResetToken()
                          ? new PasswordResetResponse(token, RESET_TOKEN_TTL.toSeconds())
                          : new PasswordResetResponse(null, RESET_TOKEN_TTL.toSeconds()));
            });
  }

  /** Completes a password reset, records history and revokes all live sessions. */
  public Mono<Void> resetPassword(ResetPasswordRequest request) {
    return resolveUserByPurposeToken(request.token(), PURPOSE_RESET)
        .flatMap(
            user ->
                passwordHistoryRepository
                    .save(new PasswordHistory(user.getId(), user.getPasswordHash(), user.getId()))
                    .then(
                        Mono.fromCallable(() -> passwordEncoder.encode(request.newPassword()))
                            .subscribeOn(Schedulers.boundedElastic()))
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
}
