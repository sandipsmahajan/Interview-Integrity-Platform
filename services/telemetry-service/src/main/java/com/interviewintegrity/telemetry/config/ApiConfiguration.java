package com.interviewintegrity.telemetry.config;

import com.interviewintegrity.telemetry.service.TelemetryEventService;
import com.interviewintegrity.telemetry.service.TelemetryEventTypeService;
import com.interviewintegrity.telemetry.service.TelemetrySessionService;
import com.interviewintegrity.telemetry.web.TelemetryEventController;
import com.interviewintegrity.telemetry.web.TelemetryEventTypeController;
import com.interviewintegrity.telemetry.web.TelemetrySessionController;
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
      TelemetryEventTypeService eventTypeService) {
    return new TelemetryEventTypeController(eventTypeService);
  }

  /** Exposes the session controller. */
  @Bean
  public TelemetrySessionController telemetrySessionController(
      TelemetrySessionService sessionService) {
    return new TelemetrySessionController(sessionService);
  }

  /** Exposes the event controller. */
  @Bean
  public TelemetryEventController telemetryEventController(TelemetryEventService eventService) {
    return new TelemetryEventController(eventService);
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
