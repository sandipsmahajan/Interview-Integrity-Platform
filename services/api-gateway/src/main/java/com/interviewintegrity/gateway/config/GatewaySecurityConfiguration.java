package com.interviewintegrity.gateway.config;

import com.interviewintegrity.security.JwtProperties;
import com.interviewintegrity.security.PlatformCors;
import com.interviewintegrity.security.PlatformJwtAuthenticationConverter;
import java.util.ArrayList;
import java.util.List;
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
    List<String> permitAll = new ArrayList<>(jwtProperties.getPermitAll());
    permitAll.addAll(
        List.of(
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/webjars/**",
            "/favicon.ico",
            "/fallback/**"));
    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .cors(spec -> spec.configurationSource(PlatformCors.from(jwtProperties)))
        .authorizeExchange(
            exchange ->
                exchange
                    .pathMatchers(permitAll.toArray(String[]::new))
                    .permitAll()
                    .pathMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus")
                    .permitAll()
                    .anyExchange()
                    .authenticated())
        .oauth2ResourceServer(
            server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter)))
        .build();
  }
}
