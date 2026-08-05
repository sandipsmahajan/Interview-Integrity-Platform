package com.integrity.featureflag.config;

import com.integrity.featureflag.service.ExperimentService;
import com.integrity.featureflag.service.FeatureFlagMapper;
import com.integrity.featureflag.service.FeatureFlagService;
import com.integrity.featureflag.service.FeatureService;
import com.integrity.featureflag.web.ExperimentController;
import com.integrity.featureflag.web.FeatureController;
import com.integrity.featureflag.web.FeatureFlagController;
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

  /** Exposes the feature controller. */
  @Bean
  public FeatureController featureController(
      FeatureService featureService, FeatureFlagMapper mapper) {
    return new FeatureController(featureService, mapper);
  }

  /** Exposes the feature flag controller. */
  @Bean
  public FeatureFlagController featureFlagController(
      FeatureFlagService flagService, FeatureFlagMapper mapper) {
    return new FeatureFlagController(flagService, mapper);
  }

  /** Exposes the experiment controller. */
  @Bean
  public ExperimentController experimentController(
      ExperimentService experimentService, FeatureFlagMapper mapper) {
    return new ExperimentController(experimentService, mapper);
  }

  /** Describes the OpenAPI document for the feature flag service. */
  @Bean
  public OpenAPI platformOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Feature Flag Service API")
                .version("v1")
                .description("Feature catalog, flag rollouts and A/B experiments"))
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
