package com.interviewintegrity.audit.web;

import com.interviewintegrity.api.PageResponse;
import com.interviewintegrity.api.PageResponses;
import com.interviewintegrity.audit.domain.ApiAuditLog;
import com.interviewintegrity.audit.service.ApiAuditLogService;
import com.interviewintegrity.audit.web.dto.ApiAuditLogResponse;
import com.interviewintegrity.security.SecurityPrincipals;
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

  /** Creates the controller bound to the API audit log service. */
  public ApiAuditLogController(ApiAuditLogService apiAuditLogService) {
    this.apiAuditLogService = apiAuditLogService;
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
                    page.items().stream().map(this::toResponse).toList(),
                    page.page(),
                    page.size(),
                    page.totalElements()));
  }

  private ApiAuditLogResponse toResponse(ApiAuditLog entry) {
    return new ApiAuditLogResponse(
        entry.getId(),
        entry.getOrganizationId(),
        entry.getMethod(),
        entry.getPath(),
        entry.getStatusCode(),
        entry.getDurationMs(),
        entry.getActorId(),
        entry.getRequestId(),
        entry.getClientIp(),
        entry.getOccurredAt());
  }
}
