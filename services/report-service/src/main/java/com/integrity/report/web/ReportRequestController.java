package com.integrity.report.web;

import com.integrity.report.service.ReportMapper;
import com.integrity.report.service.ReportRequestService;
import com.integrity.report.web.dto.CreateReportRequestRequest;
import com.integrity.report.web.dto.FailReportRequest;
import com.integrity.report.web.dto.ReportRequestResponse;
import com.integrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Report request endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/reports/{reportId}/requests")
@Tag(name = "Report Requests", description = "Manage report generation requests")
public final class ReportRequestController {

  private final ReportRequestService requestService;
  private final ReportMapper mapper;

  /** Creates the controller bound to the report request service and mapper. */
  public ReportRequestController(ReportRequestService requestService, ReportMapper mapper) {
    this.requestService = requestService;
    this.mapper = mapper;
  }

  /** Records the parameters of a report generation. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record a report request")
  public Mono<ReportRequestResponse> create(
      Authentication authentication,
      @PathVariable UUID reportId,
      @Valid @RequestBody CreateReportRequestRequest request) {
    return requestService
        .createRequest(
            reportId,
            SecurityPrincipals.organizationId(authentication),
            request.aggregationLevel().trim(),
            request.timeRange(),
            request.parameters(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Lists the requests recorded for a report. */
  @GetMapping
  @Operation(summary = "List report requests")
  public Flux<ReportRequestResponse> list(
      Authentication authentication, @PathVariable UUID reportId) {
    return requestService
        .listRequests(reportId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Returns a single request. */
  @GetMapping("/{requestId}")
  @Operation(summary = "Get a report request")
  public Mono<ReportRequestResponse> get(
      Authentication authentication, @PathVariable UUID requestId) {
    return requestService
        .getRequest(requestId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Marks a request as completed. */
  @PostMapping("/{requestId}/complete")
  @Operation(summary = "Complete a report request")
  public Mono<ReportRequestResponse> complete(
      Authentication authentication, @PathVariable UUID requestId) {
    return requestService
        .completeRequest(requestId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Marks a request as failed. */
  @PostMapping("/{requestId}/fail")
  @Operation(summary = "Fail a report request")
  public Mono<ReportRequestResponse> fail(
      Authentication authentication,
      @PathVariable UUID requestId,
      @Valid @RequestBody FailReportRequest request) {
    return requestService
        .failRequest(
            requestId,
            SecurityPrincipals.organizationId(authentication),
            request.errorMessage().trim())
        .map(mapper::toResponse);
  }
}
