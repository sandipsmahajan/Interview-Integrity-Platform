package com.interviewintegrity.identity.config;

import com.interviewintegrity.identity.repository.MfaDeviceRepository;
import com.interviewintegrity.identity.repository.OtpCodeRepository;
import com.interviewintegrity.identity.repository.PasswordHistoryRepository;
import com.interviewintegrity.identity.repository.PermissionRepository;
import com.interviewintegrity.identity.repository.RecoveryCodeRepository;
import com.interviewintegrity.identity.repository.RolePermissionRepository;
import com.interviewintegrity.identity.repository.RoleRepository;
import com.interviewintegrity.identity.repository.TrustedDeviceRepository;
import com.interviewintegrity.identity.repository.UserRepository;
import com.interviewintegrity.identity.repository.UserRoleRepository;
import com.interviewintegrity.identity.repository.UserSessionRepository;
import com.interviewintegrity.identity.service.AuthService;
import com.interviewintegrity.identity.service.AuthorityResolver;
import com.interviewintegrity.identity.service.EmailEventPublisher;
import com.interviewintegrity.identity.service.KafkaEmailEventPublisher;
import com.interviewintegrity.identity.service.KafkaUserEventPublisher;
import com.interviewintegrity.identity.service.MfaService;
import com.interviewintegrity.identity.service.OtpService;
import com.interviewintegrity.identity.service.PermissionService;
import com.interviewintegrity.identity.service.RoleService;
import com.interviewintegrity.identity.service.SessionService;
import com.interviewintegrity.identity.service.TokenIssuer;
import com.interviewintegrity.identity.service.UserEventPublisher;
import com.interviewintegrity.identity.service.UserResponseMapper;
import com.interviewintegrity.identity.service.UserService;
import com.interviewintegrity.security.JwtProperties;
import com.interviewintegrity.security.JwtTokenService;
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
  public PermissionService permissionService(PermissionRepository permissionRepository) {
    return new PermissionService(permissionRepository);
  }

  /** Provides the session management service. */
  @Bean
  public SessionService sessionService(UserSessionRepository sessionRepository) {
    return new SessionService(sessionRepository);
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
        environment.getProperty("app.auth.mfa-email-purpose", "mfa-login"));
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
      AuthProperties authProperties) {
    return new MfaService(
        mfaDeviceRepository,
        recoveryCodeRepository,
        trustedDeviceRepository,
        userRepository,
        tokenIssuer,
        jwtTokenService,
        otpService,
        authProperties);
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
