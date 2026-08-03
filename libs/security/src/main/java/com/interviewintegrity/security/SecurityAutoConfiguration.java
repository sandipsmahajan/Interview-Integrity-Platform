package com.interviewintegrity.security;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/**
 * Registers the shared security infrastructure beans: password encoding, JWT token issuing and
 * validation, and the reactive JWT to principal converter.
 */
@AutoConfiguration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityAutoConfiguration {

  /** Provides the BCrypt password encoder used to hash and verify credentials. */
  @Bean
  @ConditionalOnMissingBean(PasswordEncoder.class)
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** Provides the shared HMAC JWT token service used to issue and validate access tokens. */
  @Bean
  @ConditionalOnMissingBean(JwtTokenService.class)
  public JwtTokenService jwtTokenService(JwtProperties properties, Environment environment) {
    SecretKeys.validateForDeployment(properties.getSecret(), environment);
    return new HmacJwtTokenService(properties);
  }

  /**
   * Provides the reactive JWT decoder used by resource servers to validate incoming access tokens.
   */
  @Bean
  @ConditionalOnMissingBean(ReactiveJwtDecoder.class)
  public ReactiveJwtDecoder reactiveJwtDecoder(JwtProperties properties, Environment environment) {
    SecretKeys.validateForDeployment(properties.getSecret(), environment);
    SecretKey key =
        new SecretKeySpec(properties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    NimbusReactiveJwtDecoder decoder =
        NimbusReactiveJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    decoder.setJwtValidator(SecretKeys.defaultValidator(properties));
    return decoder;
  }

  /** Converts validated JWTs into {@link PlatformAuthenticationToken} instances. */
  @Bean
  @ConditionalOnMissingBean(PlatformJwtAuthenticationConverter.class)
  public PlatformJwtAuthenticationConverter platformJwtAuthenticationConverter() {
    return new PlatformJwtAuthenticationConverter();
  }
}
