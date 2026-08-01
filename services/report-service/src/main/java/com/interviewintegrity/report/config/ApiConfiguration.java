package com.interviewintegrity.report.config;

import com.interviewintegrity.report.service.ReportRequestService;
import com.interviewintegrity.report.service.ReportScheduleService;
import com.interviewintegrity.report.service.ReportSectionService;
import com.interviewintegrity.report.service.ReportService;
import com.interviewintegrity.report.web.ReportController;
import com.interviewintegrity.report.web.ReportRequestController;
import com.interviewintegrity.report.web.ReportScheduleController;
import com.interviewintegrity.report.web.ReportSectionController;
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

  /** Exposes the report controller. */
  @Bean
  public ReportController reportController(ReportService reportService) {
    return new ReportController(reportService);
  }

  /** Exposes the report section controller. */
  @Bean
  public ReportSectionController reportSectionController(ReportSectionService sectionService) {
    return new ReportSectionController(sectionService);
  }

  /** Exposes the report request controller. */
  @Bean
  public ReportRequestController reportRequestController(ReportRequestService requestService) {
    return new ReportRequestController(requestService);
  }

  /** Exposes the report schedule controller. */
  @Bean
  public ReportScheduleController reportScheduleController(ReportScheduleService scheduleService) {
    return new ReportScheduleController(scheduleService);
  }

  /** Describes the OpenAPI document for the report service. */
  @Bean
  public OpenAPI platformOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Report Service API")
                .version("v1")
                .description("Report generation, sections, requests and recurring schedules"))
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
