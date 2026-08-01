package com.interviewintegrity.identity.config;

import com.interviewintegrity.identity.service.AuthService;
import com.interviewintegrity.identity.service.PermissionService;
import com.interviewintegrity.identity.service.RoleService;
import com.interviewintegrity.identity.service.SessionService;
import com.interviewintegrity.identity.service.UserService;
import com.interviewintegrity.identity.web.AuthController;
import com.interviewintegrity.identity.web.PermissionController;
import com.interviewintegrity.identity.web.RoleController;
import com.interviewintegrity.identity.web.SessionController;
import com.interviewintegrity.identity.web.UserController;
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
