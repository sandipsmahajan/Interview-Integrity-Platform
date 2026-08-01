package com.interviewintegrity.configuration.config;

import com.interviewintegrity.configuration.repository.ConfigurationHistoryRepository;
import com.interviewintegrity.configuration.repository.ConfigurationRepository;
import com.interviewintegrity.configuration.repository.ConfigurationSchemaRepository;
import com.interviewintegrity.configuration.service.ConfigurationSchemaService;
import com.interviewintegrity.configuration.service.ConfigurationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit bean wiring for the configuration service application services.
 *
 * <p>Services are plain classes instantiated here rather than discovered by component scanning,
 * keeping the dependency graph visible in one place.
 */
@Configuration
public class ApplicationConfiguration {

  /** Provides the configuration schema service. */
  @Bean
  public ConfigurationSchemaService configurationSchemaService(
      ConfigurationSchemaRepository schemaRepository) {
    return new ConfigurationSchemaService(schemaRepository);
  }

  /** Provides the configuration service. */
  @Bean
  public ConfigurationService configurationService(
      ConfigurationRepository configurationRepository,
      ConfigurationHistoryRepository historyRepository) {
    return new ConfigurationService(configurationRepository, historyRepository);
  }
}
