package com.integrity.telemetry.config;

import com.integrity.security.JwtProperties;
import com.integrity.security.PlatformCors;
import com.integrity.security.PlatformJwtAuthenticationConverter;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configures the stateless JWT resource server security rules for the telemetry service.
 *
 * <p>Every API endpoint requires a valid access token; only the OpenAPI and actuator health
 * surfaces are public.
 */
@Configuration
public class SecurityConfiguration {

  /** Builds the reactive security filter chain. */
  @Bean
  public SecurityWebFilterChain securityWebFilterChain(
      ServerHttpSecurity http,
      JwtProperties jwtProperties,
      PlatformJwtAuthenticationConverter authenticationConverter) {
    List<String> permitAll =
        Stream.concat(
                jwtProperties.getPermitAll().stream(),
                Stream.of(
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/webjars/**",
                    "/favicon.ico"))
            .toList();
    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .cors(spec -> spec.configurationSource(PlatformCors.from(jwtProperties)))
        .authorizeExchange(
            exchange ->
                exchange
                    .pathMatchers(permitAll.toArray(String[]::new))
                    .permitAll()
                    .pathMatchers("/actuator/health/**", "/actuator/info/**", "/actuator/prometheus")
                    .permitAll()
                    .anyExchange()
                    .authenticated())
        .oauth2ResourceServer(
            server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter)))
        .build();
  }
}
