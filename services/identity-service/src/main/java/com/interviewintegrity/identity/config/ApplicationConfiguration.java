package com.interviewintegrity.identity.config;

import com.interviewintegrity.identity.repository.PasswordHistoryRepository;
import com.interviewintegrity.identity.repository.PermissionRepository;
import com.interviewintegrity.identity.repository.RolePermissionRepository;
import com.interviewintegrity.identity.repository.RoleRepository;
import com.interviewintegrity.identity.repository.UserRepository;
import com.interviewintegrity.identity.repository.UserRoleRepository;
import com.interviewintegrity.identity.repository.UserSessionRepository;
import com.interviewintegrity.identity.service.AuthService;
import com.interviewintegrity.identity.service.AuthorityResolver;
import com.interviewintegrity.identity.service.KafkaUserEventPublisher;
import com.interviewintegrity.identity.service.PermissionService;
import com.interviewintegrity.identity.service.RoleService;
import com.interviewintegrity.identity.service.SessionService;
import com.interviewintegrity.identity.service.UserEventPublisher;
import com.interviewintegrity.identity.service.UserResponseMapper;
import com.interviewintegrity.identity.service.UserService;
import com.interviewintegrity.security.JwtProperties;
import com.interviewintegrity.security.JwtTokenService;
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
      JwtProperties jwtProperties,
      AuthorityResolver authorityResolver,
      UserEventPublisher eventPublisher) {
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
        jwtProperties,
        authorityResolver,
        eventPublisher);
  }
}
