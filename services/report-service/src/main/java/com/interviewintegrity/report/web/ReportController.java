package com.interviewintegrity.report.web;

import com.interviewintegrity.report.domain.Report;
import com.interviewintegrity.report.domain.ReportStatus;
import com.interviewintegrity.report.domain.ReportType;
import com.interviewintegrity.report.service.ReportService;
import com.interviewintegrity.report.web.dto.CreateReportRequest;
import com.interviewintegrity.report.web.dto.ReportResponse;
import com.interviewintegrity.security.SecurityPrincipals;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Report endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Manage generated reports")
public final class ReportController {

  private final ReportService reportService;

  /** Creates the controller bound to the report service. */
  public ReportController(ReportService reportService) {
    this.reportService = reportService;
  }

  /** Creates a report. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a report")
  public Mono<ReportResponse> create(
      Authentication authentication, @Valid @RequestBody CreateReportRequest request) {
    return reportService
        .createReport(
            SecurityPrincipals.organizationId(authentication),
            request.type(),
            request.title().trim(),
            request.format(),
            request.filters(),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  /** Lists the reports of the organization, optionally filtered by status and type. */
  @GetMapping
  @Operation(summary = "List reports")
  public Flux<ReportResponse> list(
      Authentication authentication,
      @RequestParam(required = false) ReportStatus status,
      @RequestParam(required = false) ReportType type) {
    return reportService
        .listReports(SecurityPrincipals.organizationId(authentication), status, type)
        .map(this::toResponse);
  }

  /** Returns a single report. */
  @GetMapping("/{reportId}")
  @Operation(summary = "Get a report")
  public Mono<ReportResponse> get(Authentication authentication, @PathVariable UUID reportId) {
    return reportService
        .getReport(reportId, SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  /** Generates a report and publishes the report-generated event. */
  @PostMapping("/{reportId}/generate")
  @Operation(summary = "Generate a report")
  public Mono<ReportResponse> generate(Authentication authentication, @PathVariable UUID reportId) {
    return reportService
        .generateReport(reportId, SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  /** Resets a report so it can be regenerated. */
  @PostMapping("/{reportId}/regenerate")
  @Operation(summary = "Regenerate a report")
  public Mono<ReportResponse> regenerate(
      Authentication authentication, @PathVariable UUID reportId) {
    return reportService
        .regenerateReport(reportId, SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  /** Expires a report so it is no longer downloadable. */
  @PostMapping("/{reportId}/expire")
  @Operation(summary = "Expire a report")
  public Mono<ReportResponse> expire(Authentication authentication, @PathVariable UUID reportId) {
    return reportService
        .expireReport(reportId, SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  private ReportResponse toResponse(Report report) {
    return new ReportResponse(
        report.getId(),
        report.getOrganizationId(),
        report.getType(),
        report.getTitle(),
        report.getStatus(),
        report.getFormat(),
        report.getScore(),
        report.getFilters(),
        report.getRequestedBy(),
        report.getRequestedAt(),
        report.getGeneratedAt(),
        report.getExpiresAt(),
        report.getStorageObjectId(),
        report.getCreatedAt(),
        report.getUpdatedAt());
  }
}
