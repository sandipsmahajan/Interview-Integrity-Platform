package com.interviewintegrity.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interviewintegrity.event.IdentityEmailEvent;
import com.interviewintegrity.exception.AuthenticationFailedException;
import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.identity.config.AuthProperties;
import com.interviewintegrity.identity.domain.PasswordHistory;
import com.interviewintegrity.identity.domain.Permission;
import com.interviewintegrity.identity.domain.Role;
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
import com.interviewintegrity.identity.web.dto.PasswordResetResponse;
import com.interviewintegrity.identity.web.dto.RefreshRequest;
import com.interviewintegrity.identity.web.dto.RegisterOrganizationRequest;
import com.interviewintegrity.identity.web.dto.RequestPasswordResetRequest;
import com.interviewintegrity.identity.web.dto.ResetPasswordRequest;
import com.interviewintegrity.identity.web.dto.TokenResponse;
import com.interviewintegrity.identity.web.dto.UserResponse;
import com.interviewintegrity.security.JwtTokenService;
import com.interviewintegrity.security.RefreshTokens;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Unit tests for the authentication and organization onboarding flows. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  private static final String PASSWORD = "correct-horse-battery";

  @Mock private UserRepository userRepository;
  @Mock private UserRoleRepository userRoleRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private RolePermissionRepository rolePermissionRepository;
  @Mock private PermissionRepository permissionRepository;
  @Mock private UserSessionRepository sessionRepository;
  @Mock private PasswordHistoryRepository passwordHistoryRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtTokenService jwtTokenService;
  @Mock private UserEventPublisher eventPublisher;
  @Mock private EmailEventPublisher emailEventPublisher;
  @Mock private TokenIssuer tokenIssuer;
  @Mock private MfaService mfaService;

  private AuthService authService;
  private AuthProperties authProperties;

  @BeforeEach
  void setUp() {
    authProperties =
        new AuthProperties(
            "Integrity Pro",
            "http://localhost:5173",
            Duration.ofMinutes(5),
            "mfa-login",
            true,
            Duration.ofMinutes(1),
            5,
            Duration.ofMinutes(15),
            5);
    authService =
        new AuthService(
            userRepository,
            userRoleRepository,
            roleRepository,
            rolePermissionRepository,
            permissionRepository,
            sessionRepository,
            passwordHistoryRepository,
            passwordEncoder,
            jwtTokenService,
            eventPublisher,
            emailEventPublisher,
            tokenIssuer,
            mfaService,
            authProperties);
  }

  @Test
  void registerBootstrapsAdminRoleAndReturnsTokens() {
    UUID adminId = UUID.randomUUID();
    UUID roleId = UUID.randomUUID();
    UUID permissionId = UUID.randomUUID();
    RegisterOrganizationRequest request =
        new RegisterOrganizationRequest("Acme", "Admin@Example.com", PASSWORD, "Org Admin");

    when(userRepository.countLiveByEmail("admin@example.com")).thenReturn(Mono.just(0L));
    when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-hash");
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            invocation -> {
              User user = invocation.getArgument(0);
              user.setId(adminId);
              return Mono.just(user);
            });
    when(roleRepository.findLiveByOrganizationAndCode(any(UUID.class), eq("ORG_ADMIN")))
        .thenReturn(Mono.empty());
    when(roleRepository.save(any(Role.class)))
        .thenAnswer(
            invocation -> {
              Role role = invocation.getArgument(0);
              role.setId(roleId);
              return Mono.just(role);
            });
    Permission permission = new Permission();
    permission.setId(permissionId);
    when(permissionRepository.findAllOrdered()).thenReturn(Flux.just(permission));
    when(rolePermissionRepository.grant(any(), any(), any())).thenReturn(Mono.empty());
    when(userRoleRepository.assign(any(), any(), any())).thenReturn(Mono.empty());
    when(jwtTokenService.issuePurposeToken(eq("email-verify"), eq(adminId), any()))
        .thenReturn("verify-token");
    when(emailEventPublisher.publish(any(IdentityEmailEvent.class))).thenReturn(Mono.empty());
    when(tokenIssuer.issue(any(), any(), any(), any()))
        .thenReturn(
            Mono.just(
                tokenResponse("signed-access-token", "admin@example.com", "ORG_ADMIN", adminId)));
    when(eventPublisher.publishUserRegistered(any(User.class))).thenReturn(Mono.empty());

    TokenResponse response = authService.register(request).block();

    assertThat(response).isNotNull();
    assertThat(response.accessToken()).isEqualTo("signed-access-token");
    assertThat(response.refreshToken()).isNotBlank();
    assertThat(response.user().email()).isEqualTo("admin@example.com");
    assertThat(response.user().roles()).containsExactly("ORG_ADMIN");
    verify(passwordEncoder).encode(PASSWORD);
    verify(userRoleRepository).assign(adminId, roleId, adminId);
    verify(rolePermissionRepository).grant(roleId, permissionId, adminId);
    verify(emailEventPublisher, org.mockito.Mockito.times(2))
        .publish(any(IdentityEmailEvent.class));
  }

  @Test
  void registerRejectsAlreadyRegisteredEmail() {
    RegisterOrganizationRequest request =
        new RegisterOrganizationRequest("Acme", "admin@example.com", PASSWORD, "Org Admin");
    when(userRepository.countLiveByEmail("admin@example.com")).thenReturn(Mono.just(1L));

    assertThatThrownBy(() -> authService.register(request).block())
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void loginSucceedsForActiveUserWithValidPassword() {
    UUID userId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    User user = new User(organizationId, "alice@example.com", "hash", "Alice");
    user.activate();
    user.setId(userId);
    LoginRequest request = new LoginRequest("alice@example.com", PASSWORD, null, "dev", "agent");

    when(userRepository.findLiveByEmail("alice@example.com")).thenReturn(Flux.just(user));
    when(passwordEncoder.matches(PASSWORD, "hash")).thenReturn(true);
    when(mfaService.hasVerifiedDevice(userId)).thenReturn(Mono.just(false));
    when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));
    when(tokenIssuer.issue(any(), any(), any(), any()))
        .thenReturn(
            Mono.just(
                tokenResponse("signed-access-token", "alice@example.com", "RECRUITER", userId)));

    LoginResult result = authService.login(request, "127.0.0.1").block();

    assertThat(result).isInstanceOf(LoginResult.Authenticated.class);
    TokenResponse response = ((LoginResult.Authenticated) result).tokens();
    assertThat(response.accessToken()).isEqualTo("signed-access-token");
    assertThat(response.user().roles()).containsExactly("RECRUITER");
    assertThat(user.getLastLoginAt()).isNotNull();
  }

  @Test
  void loginReturnsChallengeWhenMfaRequiredAndDeviceUnknown() {
    UUID userId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    User user = new User(organizationId, "alice@example.com", "hash", "Alice");
    user.activate();
    user.setId(userId);
    LoginRequest request = new LoginRequest("alice@example.com", PASSWORD, null, "dev", "agent");

    when(userRepository.findLiveByEmail("alice@example.com")).thenReturn(Flux.just(user));
    when(passwordEncoder.matches(PASSWORD, "hash")).thenReturn(true);
    when(mfaService.hasVerifiedDevice(userId)).thenReturn(Mono.just(true));
    when(mfaService.isTrustedDevice(userId, "dev")).thenReturn(Mono.just(false));
    when(mfaService.generateChallenge(user))
        .thenReturn(
            Mono.just(
                new com.interviewintegrity.identity.web.dto.MfaChallengeResponse(
                    true, "challenge-id", 300, List.of("TOTP", "EMAIL", "RECOVERY"))));

    LoginResult result = authService.login(request, "127.0.0.1").block();

    assertThat(result).isInstanceOf(LoginResult.MfaRequired.class);
    assertThat(((LoginResult.MfaRequired) result).challenge().challengeId())
        .isEqualTo("challenge-id");
  }

  @Test
  void loginSkipsChallengeForTrustedDevice() {
    UUID userId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    User user = new User(organizationId, "alice@example.com", "hash", "Alice");
    user.activate();
    user.setId(userId);
    LoginRequest request = new LoginRequest("alice@example.com", PASSWORD, null, "dev", "agent");

    when(userRepository.findLiveByEmail("alice@example.com")).thenReturn(Flux.just(user));
    when(passwordEncoder.matches(PASSWORD, "hash")).thenReturn(true);
    when(mfaService.hasVerifiedDevice(userId)).thenReturn(Mono.just(true));
    when(mfaService.isTrustedDevice(userId, "dev")).thenReturn(Mono.just(true));
    when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));
    when(tokenIssuer.issue(any(), any(), any(), any()))
        .thenReturn(
            Mono.just(
                tokenResponse("signed-access-token", "alice@example.com", "RECRUITER", userId)));

    LoginResult result = authService.login(request, "127.0.0.1").block();

    assertThat(result).isInstanceOf(LoginResult.Authenticated.class);
    verify(tokenIssuer).issue(any(), any(), any(), any());
  }

  @Test
  void loginFailsForWrongPassword() {
    User user = new User(UUID.randomUUID(), "alice@example.com", "hash", "Alice");
    user.activate();
    LoginRequest request =
        new LoginRequest("alice@example.com", "wrong-password", null, null, null);

    when(userRepository.findLiveByEmail("alice@example.com")).thenReturn(Flux.just(user));
    when(passwordEncoder.matches("wrong-password", "hash")).thenReturn(false);
    when(userRepository.save(any(User.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    assertThatThrownBy(() -> authService.login(request, "127.0.0.1").block())
        .isInstanceOf(AuthenticationFailedException.class)
        .hasMessage("Invalid credentials");
  }

  @Test
  void loginFailsWithoutOrganizationHintForAmbiguousEmail() {
    User first = new User(UUID.randomUUID(), "alice@example.com", "hash", "Alice");
    User second = new User(UUID.randomUUID(), "alice@example.com", "hash", "Alice Two");
    LoginRequest request = new LoginRequest("alice@example.com", PASSWORD, null, null, null);

    when(userRepository.findLiveByEmail("alice@example.com")).thenReturn(Flux.just(first, second));

    assertThatThrownBy(() -> authService.login(request, "127.0.0.1").block())
        .isInstanceOf(AuthenticationFailedException.class)
        .hasMessage("Email exists in multiple organizations; specify organization id");
  }

  @Test
  void loginFailsForLockedAccount() {
    User user = new User(UUID.randomUUID(), "alice@example.com", "hash", "Alice");
    user.activate();
    user.lock();
    LoginRequest request = new LoginRequest("alice@example.com", PASSWORD, null, null, null);

    when(userRepository.findLiveByEmail("alice@example.com")).thenReturn(Flux.just(user));

    assertThatThrownBy(() -> authService.login(request, "127.0.0.1").block())
        .isInstanceOf(AuthenticationFailedException.class)
        .hasMessage("Account is locked");
  }

  @Test
  void loginFailsWhileTemporarilyLockedOut() {
    User user = new User(UUID.randomUUID(), "alice@example.com", "hash", "Alice");
    user.activate();
    user.recordFailedLogin(5, Duration.ofMinutes(15));
    user.recordFailedLogin(5, Duration.ofMinutes(15));
    user.recordFailedLogin(5, Duration.ofMinutes(15));
    user.recordFailedLogin(5, Duration.ofMinutes(15));
    user.recordFailedLogin(5, Duration.ofMinutes(15));
    LoginRequest request = new LoginRequest("alice@example.com", PASSWORD, null, null, null);

    when(userRepository.findLiveByEmail("alice@example.com")).thenReturn(Flux.just(user));

    assertThatThrownBy(() -> authService.login(request, "127.0.0.1").block())
        .isInstanceOf(AuthenticationFailedException.class)
        .hasMessage("Account is temporarily locked");
  }

  @Test
  void repeatedFailedLoginsLockTheAccount() {
    User user = new User(UUID.randomUUID(), "alice@example.com", "hash", "Alice");
    user.activate();
    LoginRequest request = new LoginRequest("alice@example.com", "wrong", null, null, null);

    when(userRepository.findLiveByEmail("alice@example.com")).thenReturn(Flux.just(user));
    when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
    when(userRepository.save(any(User.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    for (int attempt = 0; attempt < 5; attempt++) {
      try {
        authService.login(request, "127.0.0.1").block();
      } catch (AuthenticationFailedException expected) {
        // each failed attempt increments the counter
      }
    }

    assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
    assertThat(user.isLockedOut()).isTrue();
  }

  @Test
  void refreshRotatesSessionAndReturnsFreshTokens() {
    UUID userId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    User user = new User(organizationId, "alice@example.com", "hash", "Alice");
    user.activate();
    user.setId(userId);
    String rawToken = RefreshTokens.generate();
    UserSession session =
        new UserSession(
            userId,
            organizationId,
            RefreshTokens.hash(rawToken),
            "dev",
            "127.0.0.1",
            "agent",
            Instant.now().plus(30, ChronoUnit.DAYS));

    when(sessionRepository.findByRefreshTokenHash(any())).thenReturn(Mono.just(session));
    when(sessionRepository.save(any(UserSession.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(userRepository.findLiveById(userId)).thenReturn(Mono.just(user));
    when(tokenIssuer.issue(any(), any(), any(), any()))
        .thenReturn(
            Mono.just(
                tokenResponse("rotated-access-token", "alice@example.com", "RECRUITER", userId)));

    TokenResponse response = authService.refresh(new RefreshRequest(rawToken)).block();

    assertThat(response).isNotNull();
    assertThat(response.accessToken()).isEqualTo("rotated-access-token");
  }

  @Test
  void refreshRejectsUnknownToken() {
    when(sessionRepository.findByRefreshTokenHash(any())).thenReturn(Mono.empty());

    assertThatThrownBy(() -> authService.refresh(new RefreshRequest("unknown")).block())
        .isInstanceOf(AuthenticationFailedException.class);
  }

  @Test
  void refreshReuseRevokesAllSessions() {
    UUID userId = UUID.randomUUID();
    UserSession revoked =
        new UserSession(
            userId,
            UUID.randomUUID(),
            "revoked-hash",
            "dev",
            "127.0.0.1",
            "agent",
            Instant.now().plus(30, ChronoUnit.DAYS));
    revoked.revoke(null);

    when(sessionRepository.findByRefreshTokenHash(any())).thenReturn(Mono.just(revoked));
    when(sessionRepository.revokeAllActiveByUser(eq(userId), any(Instant.class)))
        .thenReturn(Mono.just(2));

    assertThatThrownBy(() -> authService.refresh(new RefreshRequest("stale-token")).block())
        .isInstanceOf(AuthenticationFailedException.class)
        .hasMessage("Refresh token no longer valid");
    verify(sessionRepository).revokeAllActiveByUser(eq(userId), any(Instant.class));
  }

  @Test
  void requestPasswordResetReturnsTokenForSingleAccount() {
    UUID userId = UUID.randomUUID();
    User user = new User(UUID.randomUUID(), "alice@example.com", "hash", "Alice");
    user.setId(userId);

    when(userRepository.findLiveByEmail("alice@example.com")).thenReturn(Flux.just(user));
    when(userRepository.save(any(User.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(jwtTokenService.issuePurposeToken(eq("password-reset"), eq(userId), any()))
        .thenReturn("reset-token");
    when(emailEventPublisher.publish(any(IdentityEmailEvent.class))).thenReturn(Mono.empty());

    PasswordResetResponse response =
        authService
            .requestPasswordReset(new RequestPasswordResetRequest("alice@example.com"))
            .block();

    assertThat(response).isNotNull();
    assertThat(response.resetToken()).isEqualTo("reset-token");
    assertThat(response.expiresInSeconds()).isEqualTo(900);
    verify(emailEventPublisher).publish(any(IdentityEmailEvent.class));
  }

  @Test
  void requestPasswordResetOmitsTokenForAmbiguousEmail() {
    User first = new User(UUID.randomUUID(), "alice@example.com", "hash", "Alice");
    User second = new User(UUID.randomUUID(), "alice@example.com", "hash", "Alice Two");

    when(userRepository.findLiveByEmail("alice@example.com")).thenReturn(Flux.just(first, second));

    PasswordResetResponse response =
        authService
            .requestPasswordReset(new RequestPasswordResetRequest("alice@example.com"))
            .block();

    assertThat(response).isNotNull();
    assertThat(response.resetToken()).isNull();
  }

  @Test
  void requestPasswordResetHidesTokenWhenExposureDisabled() {
    authProperties =
        new AuthProperties(
            "Integrity Pro",
            "http://localhost:5173",
            Duration.ofMinutes(5),
            "mfa-login",
            false,
            Duration.ofMinutes(1),
            5,
            Duration.ofMinutes(15),
            5);
    authService =
        new AuthService(
            userRepository,
            userRoleRepository,
            roleRepository,
            rolePermissionRepository,
            permissionRepository,
            sessionRepository,
            passwordHistoryRepository,
            passwordEncoder,
            jwtTokenService,
            eventPublisher,
            emailEventPublisher,
            tokenIssuer,
            mfaService,
            authProperties);

    UUID userId = UUID.randomUUID();
    User user = new User(UUID.randomUUID(), "alice@example.com", "hash", "Alice");
    user.setId(userId);

    when(userRepository.findLiveByEmail("alice@example.com")).thenReturn(Flux.just(user));
    when(userRepository.save(any(User.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(jwtTokenService.issuePurposeToken(eq("password-reset"), eq(userId), any()))
        .thenReturn("reset-token");
    when(emailEventPublisher.publish(any(IdentityEmailEvent.class))).thenReturn(Mono.empty());

    PasswordResetResponse response =
        authService
            .requestPasswordReset(new RequestPasswordResetRequest("alice@example.com"))
            .block();

    assertThat(response).isNotNull();
    assertThat(response.resetToken()).isNull();
    verify(emailEventPublisher).publish(any(IdentityEmailEvent.class));
  }

  @Test
  void requestPasswordResetThrottlesRepeatRequests() {
    UUID userId = UUID.randomUUID();
    User user = new User(UUID.randomUUID(), "alice@example.com", "hash", "Alice");
    user.setId(userId);
    user.recordPasswordResetRequest();

    when(userRepository.findLiveByEmail("alice@example.com")).thenReturn(Flux.just(user));

    PasswordResetResponse response =
        authService
            .requestPasswordReset(new RequestPasswordResetRequest("alice@example.com"))
            .block();

    assertThat(response).isNotNull();
    assertThat(response.resetToken()).isNull();
    assertThat(response.expiresInSeconds()).isZero();
    verify(emailEventPublisher, never()).publish(any(IdentityEmailEvent.class));
  }

  @Test
  void resetPasswordRevokesAllActiveSessions() {
    UUID userId = UUID.randomUUID();
    User user = new User(UUID.randomUUID(), "alice@example.com", "hash", "Alice");
    user.activate();
    user.setId(userId);

    when(jwtTokenService.resolvePurposeToken("reset-token", "password-reset")).thenReturn(userId);
    when(userRepository.findLiveById(userId)).thenReturn(Mono.just(user));
    when(passwordHistoryRepository.save(any()))
        .thenReturn(Mono.just(new PasswordHistory(userId, "hash", userId)));
    when(passwordEncoder.encode("new-password")).thenReturn("new-hash");
    when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));
    when(sessionRepository.revokeAllActiveByUser(eq(userId), any(Instant.class)))
        .thenReturn(Mono.just(1));

    authService.resetPassword(new ResetPasswordRequest("reset-token", "new-password")).block();

    assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    verify(sessionRepository).revokeAllActiveByUser(eq(userId), any(Instant.class));
  }

  private static TokenResponse tokenResponse(
      String accessToken, String email, String role, UUID userId) {
    UserResponse userResponse =
        new UserResponse(
            userId,
            UUID.randomUUID(),
            email,
            "Alice",
            "ACTIVE",
            Instant.now(),
            Instant.now(),
            Instant.now(),
            List.of(role));
    return new TokenResponse(accessToken, "refresh-token", 900, "Bearer", userResponse);
  }
}
