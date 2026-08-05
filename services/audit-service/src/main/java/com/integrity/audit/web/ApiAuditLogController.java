package com.integrity.audit.web;

import com.integrity.api.PageResponse;
import com.integrity.api.PageResponses;
import com.integrity.audit.service.ApiAuditLogService;
import com.integrity.audit.service.AuditMapper;
import com.integrity.audit.web.dto.ApiAuditLogResponse;
import com.integrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** HTTP access log endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/api-audit-log")
@Tag(name = "API Audit Log", description = "HTTP access log")
public final class ApiAuditLogController {

  private final ApiAuditLogService apiAuditLogService;
  private final AuditMapper mapper;

  /** Creates the controller bound to the API audit log service and mapper. */
  public ApiAuditLogController(ApiAuditLogService apiAuditLogService, AuditMapper mapper) {
    this.apiAuditLogService = apiAuditLogService;
    this.mapper = mapper;
  }

  /** Lists the access log entries of the organization. */
  @GetMapping
  @Operation(summary = "List API audit log")
  public Mono<PageResponse<ApiAuditLogResponse>> list(
      Authentication authentication,
      @RequestParam(required = false) String method,
      Pageable pageable) {
    return apiAuditLogService
        .list(SecurityPrincipals.organizationId(authentication), method, pageable)
        .map(
            page ->
                PageResponses.of(
                    page.items().stream().map(mapper::toResponse).toList(),
                    page.page(),
                    page.size(),
                    page.totalElements()));
  }
}
