package com.integrity.audit.config;

import com.integrity.audit.service.ApiAuditLogService;
import com.integrity.audit.service.AuditMapper;
import com.integrity.audit.service.AuditService;
import com.integrity.audit.web.ApiAuditLogController;
import com.integrity.audit.web.AuditEventController;
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
  public AuditEventController auditEventController(AuditService auditService, AuditMapper mapper) {
    return new AuditEventController(auditService, mapper);
  }

  /** Exposes the API audit log controller. */
  @Bean
  public ApiAuditLogController apiAuditLogController(
      ApiAuditLogService apiAuditLogService, AuditMapper mapper) {
    return new ApiAuditLogController(apiAuditLogService, mapper);
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
