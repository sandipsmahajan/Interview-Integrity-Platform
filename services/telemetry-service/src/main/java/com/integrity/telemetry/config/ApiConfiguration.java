package com.integrity.telemetry.config;

import com.integrity.telemetry.service.TelemetryEventService;
import com.integrity.telemetry.service.TelemetryEventTypeService;
import com.integrity.telemetry.service.TelemetryMapper;
import com.integrity.telemetry.service.TelemetrySessionService;
import com.integrity.telemetry.web.TelemetryEventController;
import com.integrity.telemetry.web.TelemetryEventTypeController;
import com.integrity.telemetry.web.TelemetrySessionController;
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

  /** Exposes the event type controller. */
  @Bean
  public TelemetryEventTypeController telemetryEventTypeController(
      TelemetryEventTypeService eventTypeService, TelemetryMapper mapper) {
    return new TelemetryEventTypeController(eventTypeService, mapper);
  }

  /** Exposes the session controller. */
  @Bean
  public TelemetrySessionController telemetrySessionController(
      TelemetrySessionService sessionService, TelemetryMapper mapper) {
    return new TelemetrySessionController(sessionService, mapper);
  }

  /** Exposes the event controller. */
  @Bean
  public TelemetryEventController telemetryEventController(
      TelemetryEventService eventService, TelemetryMapper mapper) {
    return new TelemetryEventController(eventService, mapper);
  }

  /** Describes the OpenAPI document for the telemetry service. */
  @Bean
  public OpenAPI platformOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Telemetry Service API")
                .version("v1")
                .description("Captures and reports exam telemetry events"))
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
