package com.integrity.discovery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Secures the Eureka server.
 *
 * <p>The registry endpoints under {@code /eureka/**} must remain reachable by every service so
 * instances can register, heartbeat and fetch the registry. The dashboard and actuator health
 * surfaces are also public; everything else is denied by default.
 */
@Configuration
@EnableWebSecurity
public class EurekaSecurityConfiguration {

  private static final String EUREKA_ENDPOINTS = "/eureka/**";

  /** Builds the servlet security filter chain for the Eureka server. */
  @Bean
  public SecurityFilterChain eurekaSecurityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        HttpMethod.GET, "/", EUREKA_ENDPOINTS, "/static/**", "/webjars/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, EUREKA_ENDPOINTS)
                    .permitAll()
                    .requestMatchers(HttpMethod.PUT, EUREKA_ENDPOINTS)
                    .permitAll()
                    .requestMatchers(HttpMethod.DELETE, EUREKA_ENDPOINTS)
                    .permitAll()
                    .requestMatchers(
                        "/actuator/health",
                        "/actuator/health/**",
                        "/actuator/info",
                        "/actuator/prometheus",
                        "/favicon.ico")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        .httpBasic(basic -> basic.disable())
        .formLogin(form -> form.disable())
        .build();
  }
}
