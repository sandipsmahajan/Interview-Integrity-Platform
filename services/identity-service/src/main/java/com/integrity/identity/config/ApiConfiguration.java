package com.integrity.identity.config;

import com.integrity.identity.repository.UserRepository;
import com.integrity.identity.service.AuthService;
import com.integrity.identity.service.MfaService;
import com.integrity.identity.service.OtpService;
import com.integrity.identity.service.PermissionService;
import com.integrity.identity.service.RoleService;
import com.integrity.identity.service.SessionService;
import com.integrity.identity.service.UserService;
import com.integrity.identity.web.AuthController;
import com.integrity.identity.web.MfaController;
import com.integrity.identity.web.OtpController;
import com.integrity.identity.web.PermissionController;
import com.integrity.identity.web.RoleController;
import com.integrity.identity.web.SessionController;
import com.integrity.identity.web.UserController;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the REST controllers as beans and describes the OpenAPI surface of the service. */
@Configuration
public class ApiConfiguration {

  /** Exposes the authentication controller. */
  @Bean
  public AuthController authController(AuthService authService) {
    return new AuthController(authService);
  }

  /** Exposes the user controller. */
  @Bean
  public UserController userController(UserService userService) {
    return new UserController(userService);
  }

  /** Exposes the role controller. */
  @Bean
  public RoleController roleController(RoleService roleService) {
    return new RoleController(roleService);
  }

  /** Exposes the permission controller. */
  @Bean
  public PermissionController permissionController(PermissionService permissionService) {
    return new PermissionController(permissionService);
  }

  /** Exposes the session controller. */
  @Bean
  public SessionController sessionController(SessionService sessionService) {
    return new SessionController(sessionService);
  }

  /** Exposes the email OTP controller. */
  @Bean
  public OtpController otpController(OtpService otpService, UserRepository userRepository) {
    return new OtpController(otpService, userRepository);
  }

  /** Exposes the multi-factor authentication controller. */
  @Bean
  public MfaController mfaController(MfaService mfaService, UserRepository userRepository) {
    return new MfaController(mfaService, userRepository);
  }

  /** Describes the OpenAPI document for the identity service. */
  @Bean
  public OpenAPI platformOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Identity Service API")
                .version("v1")
                .description(
                    "Identity and access management: users, roles, sessions and authentication"))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
