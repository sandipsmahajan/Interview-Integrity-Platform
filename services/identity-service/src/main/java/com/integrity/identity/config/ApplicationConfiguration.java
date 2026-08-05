package com.integrity.identity.config;

import com.integrity.identity.repository.MfaChallengeAttemptRepository;
import com.integrity.identity.repository.MfaDeviceRepository;
import com.integrity.identity.repository.OtpCodeRepository;
import com.integrity.identity.repository.PasswordHistoryRepository;
import com.integrity.identity.repository.PermissionRepository;
import com.integrity.identity.repository.RecoveryCodeRepository;
import com.integrity.identity.repository.RolePermissionRepository;
import com.integrity.identity.repository.RoleRepository;
import com.integrity.identity.repository.TrustedDeviceRepository;
import com.integrity.identity.repository.UserRepository;
import com.integrity.identity.repository.UserRoleRepository;
import com.integrity.identity.repository.UserSessionRepository;
import com.integrity.identity.service.AuthService;
import com.integrity.identity.service.AuthorityResolver;
import com.integrity.identity.service.EmailEventPublisher;
import com.integrity.identity.service.IdentityMapper;
import com.integrity.identity.service.KafkaEmailEventPublisher;
import com.integrity.identity.service.KafkaUserEventPublisher;
import com.integrity.identity.service.MfaService;
import com.integrity.identity.service.OtpService;
import com.integrity.identity.service.PermissionService;
import com.integrity.identity.service.RoleService;
import com.integrity.identity.service.SessionService;
import com.integrity.identity.service.TokenIssuer;
import com.integrity.identity.service.UserEventPublisher;
import com.integrity.identity.service.UserResponseMapper;
import com.integrity.identity.service.UserService;
import com.integrity.security.JwtProperties;
import com.integrity.security.JwtTokenService;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.kafka.sender.KafkaSender;

/**
 * Explicit bean wiring for the identity service application services.
 *
 * <p>Services are plain classes instantiated here rather than discovered by component scanning,
 * keeping the dependency graph visible in one place.
 */
@Configuration
public class ApplicationConfiguration {

  /** Provides the database client backed user-role bridge repository. */
  @Bean
  public UserRoleRepository userRoleRepository(DatabaseClient databaseClient) {
    return new UserRoleRepository(databaseClient);
  }

  /** Provides the database client backed role-permission bridge repository. */
  @Bean
  public RolePermissionRepository rolePermissionRepository(DatabaseClient databaseClient) {
    return new RolePermissionRepository(databaseClient);
  }

  /** Provides the authority resolver. */
  @Bean
  public AuthorityResolver authorityResolver(
      UserRoleRepository userRoleRepository, PermissionRepository permissionRepository) {
    return new AuthorityResolver(userRoleRepository, permissionRepository);
  }

  /** Provides the user response mapper. */
  @Bean
  public UserResponseMapper userResponseMapper(UserRoleRepository userRoleRepository) {
    return new UserResponseMapper(userRoleRepository);
  }

  /** Provides the user management service. */
  @Bean
  public UserService userService(
      UserRepository userRepository,
      UserRoleRepository userRoleRepository,
      RoleRepository roleRepository,
      UserResponseMapper responseMapper) {
    return new UserService(userRepository, userRoleRepository, roleRepository, responseMapper);
  }

  /** Provides the role management service. */
  @Bean
  public RoleService roleService(
      RoleRepository roleRepository,
      RolePermissionRepository rolePermissionRepository,
      PermissionRepository permissionRepository) {
    return new RoleService(roleRepository, rolePermissionRepository, permissionRepository);
  }

  /** Provides the permission catalog service. */
  @Bean
  public PermissionService permissionService(
      PermissionRepository permissionRepository, IdentityMapper mapper) {
    return new PermissionService(permissionRepository, mapper);
  }

  /** Provides the session management service. */
  @Bean
  public SessionService sessionService(
      UserSessionRepository sessionRepository, IdentityMapper mapper) {
    return new SessionService(sessionRepository, mapper);
  }

  /** Provides the event publisher for user lifecycle events. */
  @Bean
  public UserEventPublisher userEventPublisher(
      KafkaSender<String, String> sender, Environment environment) {
    String serviceName = environment.getProperty("spring.application.name", "identity-service");
    return new KafkaUserEventPublisher(sender, serviceName);
  }

  /** Provides the event publisher for email delivery requests. */
  @Bean
  public EmailEventPublisher emailEventPublisher(
      KafkaSender<String, String> sender, Environment environment) {
    String serviceName = environment.getProperty("spring.application.name", "identity-service");
    return new KafkaEmailEventPublisher(sender, serviceName);
  }

  /** Provides the token issuer shared by auth and MFA flows. */
  @Bean
  public TokenIssuer tokenIssuer(
      AuthorityResolver authorityResolver,
      JwtTokenService jwtTokenService,
      JwtProperties jwtProperties,
      UserSessionRepository sessionRepository) {
    return new TokenIssuer(authorityResolver, jwtTokenService, jwtProperties, sessionRepository);
  }

  /** Provides the email OTP service. */
  @Bean
  public OtpService otpService(
      OtpCodeRepository otpCodeRepository, EmailEventPublisher emailEventPublisher) {
    return new OtpService(otpCodeRepository, emailEventPublisher);
  }

  /** Provides the authentication flow properties. */
  @Bean
  public AuthProperties authProperties(Environment environment) {
    return new AuthProperties(
        environment.getProperty("app.auth.app-name", "Integrity Pro"),
        environment.getProperty("app.auth.frontend-base-url", "http://localhost:5173"),
        Duration.parse(environment.getProperty("app.auth.mfa-challenge-ttl", "PT5M")),
        environment.getProperty("app.auth.mfa-email-purpose", "mfa-login"),
        environment.getProperty("app.auth.expose-reset-token", Boolean.class, false),
        Duration.parse(environment.getProperty("app.auth.reset-request-interval", "PT1M")),
        environment.getProperty("app.auth.max-login-attempts", Integer.class, 5),
        Duration.parse(environment.getProperty("app.auth.login-lockout", "PT15M")),
        environment.getProperty("app.auth.max-mfa-challenge-attempts", Integer.class, 5));
  }

  /** Provides the multi-factor authentication service. */
  @Bean
  public MfaService mfaService(
      MfaDeviceRepository mfaDeviceRepository,
      RecoveryCodeRepository recoveryCodeRepository,
      TrustedDeviceRepository trustedDeviceRepository,
      UserRepository userRepository,
      TokenIssuer tokenIssuer,
      JwtTokenService jwtTokenService,
      OtpService otpService,
      MfaChallengeAttemptRepository challengeAttemptRepository,
      AuthProperties authProperties,
      IdentityMapper mapper) {
    return new MfaService(
        mfaDeviceRepository,
        recoveryCodeRepository,
        trustedDeviceRepository,
        userRepository,
        tokenIssuer,
        jwtTokenService,
        otpService,
        challengeAttemptRepository,
        authProperties,
        mapper);
  }

  /** Provides the authentication service. */
  @Bean
  public AuthService authService(
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
    return new AuthService(
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
}
