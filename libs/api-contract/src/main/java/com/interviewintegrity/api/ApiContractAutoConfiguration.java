package com.interviewintegrity.api;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

/** Registers the shared API contract infrastructure beans: the platform error response handler. */
@AutoConfiguration
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
