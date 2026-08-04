package com.interviewintegrity.api;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.webflux.autoconfigure.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

/**
 * Registers the shared API contract infrastructure beans: the platform error response handler.
 *
 * <p>{@code @AutoConfigureBefore(ErrorWebFluxAutoConfiguration.class)} makes sure the platform
 * handler is registered before Boot evaluates its
 * {@code @ConditionalOnMissingBean(ErrorWebExceptionHandler.class)} guard, so the default reactive
 * error handler is not created and the platform error contract is the single handler in the chain.
 */
@AutoConfiguration
@AutoConfigureBefore(ErrorWebFluxAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class ApiContractAutoConfiguration {

  /**
   * Replaces the default reactive error handler with the platform {@link ErrorResponse} renderer.
   */
  @Bean
  public ErrorWebExceptionHandler platformErrorWebExceptionHandler(ObjectMapper objectMapper) {
    return new PlatformErrorWebExceptionHandler(objectMapper);
  }
}
