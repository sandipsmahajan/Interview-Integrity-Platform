package com.integrity.audit.service;

import com.integrity.api.PageResponse;
import com.integrity.api.PageResponses;
import com.integrity.audit.domain.ApiAuditLog;
import com.integrity.audit.repository.ApiAuditLogRepository;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Provides search over the HTTP access log of an organization. */
public class ApiAuditLogService {

  private final ApiAuditLogRepository apiAuditLogRepository;

  /** Wires the service with its repository. */
  public ApiAuditLogService(ApiAuditLogRepository apiAuditLogRepository) {
    this.apiAuditLogRepository = apiAuditLogRepository;
  }

  /** Lists the access log entries of an organization, optionally filtered by method. */
  @Transactional(readOnly = true)
  public Mono<PageResponse<ApiAuditLog>> list(
      UUID organizationId, String method, Pageable pageable) {
    Flux<ApiAuditLog> page;
    Mono<Long> count;
    if (method != null) {
      page = apiAuditLogRepository.listByOrganizationAndMethod(organizationId, method, pageable);
      count = apiAuditLogRepository.countByOrganizationAndMethod(organizationId, method);
    } else {
      page = apiAuditLogRepository.listByOrganization(organizationId, pageable);
      count = apiAuditLogRepository.countByOrganization(organizationId);
    }
    return page.collectList()
        .zipWith(count)
        .map(
            tuple ->
                PageResponses.of(
                    tuple.getT1(),
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    tuple.getT2()));
  }
}
