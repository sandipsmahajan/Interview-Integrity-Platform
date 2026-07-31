package com.interviewintegrity.platform.infrastructure;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {
  @Bean
  SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(
            exchanges ->
                exchanges
                    .pathMatchers("/actuator/health", "/api/v1/auth/**")
                    .permitAll()
                    .anyExchange()
                    .authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .build();
  }

  @Bean
  ReactiveJwtDecoder jwtDecoder(
      @Value("${security.jwt.hmac-secret:local-development-change-me-32-bytes-minimum}")
          String secret) {
    SecretKey key =
        new SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
    return NimbusReactiveJwtDecoder.withSecretKey(key).build();
  }
}
