package com.integrity.configuration.config;

import com.integrity.configuration.service.ConfigurationMapper;
import com.integrity.configuration.service.ConfigurationSchemaService;
import com.integrity.configuration.service.ConfigurationService;
import com.integrity.configuration.web.ConfigurationController;
import com.integrity.configuration.web.ConfigurationSchemaController;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the REST controllers as beans and describes the OpenAPI surface of the service. */
@Configuration
public class ApiConfiguration {

  /** Exposes the configuration schema controller. */
  @Bean
  public ConfigurationSchemaController configurationSchemaController(
      ConfigurationSchemaService schemaService, ConfigurationMapper mapper) {
    return new ConfigurationSchemaController(schemaService, mapper);
  }

  /** Exposes the configuration controller. */
  @Bean
  public ConfigurationController configurationController(
      ConfigurationService configurationService, ConfigurationMapper mapper) {
    return new ConfigurationController(configurationService, mapper);
  }

  /** Describes the OpenAPI document for the configuration service. */
  @Bean
  public OpenAPI platformOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Configuration Service API")
                .version("v1")
                .description("Configuration schema catalog and tenant scoped values"))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
