package com.interviewintegrity.observability;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.server.WebFilter;

/** Registers the observability infrastructure beans: the correlation id web filter. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class ObservabilityAutoConfiguration {

  /** Provides the reactive web filter that propagates the request id. */
  @Bean
  public WebFilter correlationIdWebFilter() {
    return new CorrelationIdWebFilter();
  }
}
