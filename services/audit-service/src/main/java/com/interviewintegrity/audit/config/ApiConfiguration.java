package com.interviewintegrity.audit.config;

import com.interviewintegrity.audit.service.ApiAuditLogService;
import com.interviewintegrity.audit.service.AuditService;
import com.interviewintegrity.audit.web.ApiAuditLogController;
import com.interviewintegrity.audit.web.AuditEventController;
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

  /** Exposes the audit event controller. */
  @Bean
  public AuditEventController auditEventController(AuditService auditService) {
    return new AuditEventController(auditService);
  }

  /** Exposes the API audit log controller. */
  @Bean
  public ApiAuditLogController apiAuditLogController(ApiAuditLogService apiAuditLogService) {
    return new ApiAuditLogController(apiAuditLogService);
  }

  /** Describes the OpenAPI document for the audit service. */
  @Bean
  public OpenAPI platformOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Audit Service API")
                .version("v1")
                .description("Compliance audit events and API access log"))
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
