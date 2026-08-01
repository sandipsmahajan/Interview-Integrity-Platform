package com.interviewintegrity.discovery.config;

import com.interviewintegrity.discovery.service.DiscoveryService;
import com.interviewintegrity.discovery.web.DiscoveryController;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the discovery application services and describes its OpenAPI surface. */
@Configuration
public class ApplicationConfiguration {

  /** Provides the in-memory registry with a heartbeat timeout. */
  @Bean
  public DiscoveryService discoveryService(
      @Value("${discovery.heartbeat-timeout-seconds:60}") long heartbeatTimeoutSeconds) {
    return new DiscoveryService(heartbeatTimeoutSeconds);
  }

  /** Exposes the discovery controller. */
  @Bean
  public DiscoveryController discoveryController(DiscoveryService discoveryService) {
    return new DiscoveryController(discoveryService);
  }

  /** Describes the OpenAPI document for the discovery service. */
  @Bean
  public OpenAPI platformOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Discovery Service API")
                .version("v1")
                .description("Lightweight service registry with heartbeat eviction"))
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
