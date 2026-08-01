package com.interviewintegrity.audit.config;

import com.interviewintegrity.audit.repository.ApiAuditLogRepository;
import com.interviewintegrity.audit.repository.AuditEventChangeRepository;
import com.interviewintegrity.audit.repository.AuditEventRepository;
import com.interviewintegrity.audit.service.ApiAuditLogService;
import com.interviewintegrity.audit.service.AuditService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit bean wiring for the audit service application services.
 *
 * <p>Services are plain classes instantiated here rather than discovered by component scanning,
 * keeping the dependency graph visible in one place.
 */
@Configuration
public class ApplicationConfiguration {

  /** Provides the audit service. */
  @Bean
  public AuditService auditService(
      AuditEventRepository auditEventRepository, AuditEventChangeRepository changeRepository) {
    return new AuditService(auditEventRepository, changeRepository);
  }

  /** Provides the API audit log service. */
  @Bean
  public ApiAuditLogService apiAuditLogService(ApiAuditLogRepository apiAuditLogRepository) {
    return new ApiAuditLogService(apiAuditLogRepository);
  }
}
