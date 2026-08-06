package com.integrity.identity.config;

import com.integrity.security.JwtProperties;
import com.integrity.security.PlatformCors;
import com.integrity.security.PlatformJwtAuthenticationConverter;
import com.integrity.security.PublicAuthPaths;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configures the stateless JWT resource server security rules for the identity service.
 *
 * <p>Public endpoints are the authentication and password recovery flows plus the OpenAPI and
 * actuator health surfaces; everything else requires a valid access token. Identity management
 * endpoints are additionally gated on the permission codes embedded in the access token so that
 * only users holding {@code identity.users.read}, {@code identity.users.write} or {@code
 * identity.roles.manage} can administer accounts and roles.
 */
@Configuration
public class SecurityConfiguration {

  private static final String AUTHORITY_READ_USERS = "identity.users.read";
  private static final String AUTHORITY_WRITE_USERS = "identity.users.write";
  private static final String AUTHORITY_MANAGE_ROLES = "identity.roles.manage";
  private static final String PATH_USERS = "/api/v1/users/**";

  /** Builds the reactive security filter chain. */
  @Bean
  public SecurityWebFilterChain securityWebFilterChain(
      ServerHttpSecurity http,
      JwtProperties jwtProperties,
      PlatformJwtAuthenticationConverter authenticationConverter) {
    List<String> permitAll =
        Stream.concat(
                jwtProperties.getPermitAll().stream(),
                Stream.concat(
                    PublicAuthPaths.PATHS.stream(),
                    Stream.of(
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/webjars/**",
                        "/favicon.ico")))
            .toList();
    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .cors(spec -> spec.configurationSource(PlatformCors.from(jwtProperties)))
        .authorizeExchange(
            exchange ->
                exchange
                    .pathMatchers(permitAll.toArray(String[]::new))
                    .permitAll()
                    .pathMatchers(
                        "/actuator/health/**", "/actuator/info/**", "/actuator/prometheus")
                    .permitAll()
                    .pathMatchers(HttpMethod.GET, PATH_USERS)
                    .hasAuthority(AUTHORITY_READ_USERS)
                    .pathMatchers(HttpMethod.POST, PATH_USERS)
                    .hasAuthority(AUTHORITY_WRITE_USERS)
                    .pathMatchers(HttpMethod.PATCH, PATH_USERS)
                    .hasAuthority(AUTHORITY_WRITE_USERS)
                    .pathMatchers(HttpMethod.DELETE, PATH_USERS)
                    .hasAuthority(AUTHORITY_WRITE_USERS)
                    .pathMatchers("/api/v1/roles/**", "/api/v1/permissions/**")
                    .hasAnyAuthority(AUTHORITY_MANAGE_ROLES, AUTHORITY_WRITE_USERS)
                    .anyExchange()
                    .authenticated())
        .oauth2ResourceServer(
            server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter)))
        .build();
  }
}
