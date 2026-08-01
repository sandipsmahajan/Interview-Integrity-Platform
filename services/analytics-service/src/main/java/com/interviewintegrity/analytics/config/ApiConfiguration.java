package com.interviewintegrity.analytics.config;

import com.interviewintegrity.analytics.service.AnalyticsJobRunService;
import com.interviewintegrity.analytics.service.AnalyticsService;
import com.interviewintegrity.analytics.web.AnalyticsController;
import com.interviewintegrity.analytics.web.AnalyticsJobRunController;
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

  /** Exposes the analytics controller. */
  @Bean
  public AnalyticsController analyticsController(AnalyticsService analyticsService) {
    return new AnalyticsController(analyticsService);
  }

  /** Exposes the analytics job run controller. */
  @Bean
  public AnalyticsJobRunController analyticsJobRunController(AnalyticsJobRunService jobRunService) {
    return new AnalyticsJobRunController(jobRunService);
  }

  /** Describes the OpenAPI document for the analytics service. */
  @Bean
  public OpenAPI platformOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Analytics Service API")
                .version("v1")
                .description("Pre-aggregated daily summaries and aggregation job runs"))
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
