package com.integrity.gateway.config;

import com.integrity.security.JwtProperties;
import com.integrity.security.PlatformCors;
import com.integrity.security.PlatformJwtAuthenticationConverter;
import com.integrity.security.PublicAuthPaths;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configures the stateless JWT resource server security rules for the API gateway.
 *
 * <p>Every routed request must carry a valid access token; only the OpenAPI, fallback and actuator
 * health surfaces are public. CORS is applied from the shared platform properties.
 */
@Configuration
public class GatewaySecurityConfiguration {

  /** Builds the reactive security filter chain for the gateway. */
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
                        "/favicon.ico",
                        "/fallback/**")))
            .toList();
    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .cors(spec -> spec.configurationSource(PlatformCors.from(jwtProperties)))
        .authorizeExchange(
            exchange ->
                exchange
                    .pathMatchers(permitAll.toArray(String[]::new))
                    .permitAll()
                    .pathMatchers(
                        "/actuator/health/readiness", "/actuator/health/liveness", "/actuator/info")
                    .permitAll()
                    .anyExchange()
                    .authenticated())
        .oauth2ResourceServer(
            server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter)))
        .build();
  }
}
