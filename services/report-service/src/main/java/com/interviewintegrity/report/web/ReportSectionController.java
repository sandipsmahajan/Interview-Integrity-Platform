package com.interviewintegrity.report.web;

import com.interviewintegrity.report.service.ReportMapper;
import com.interviewintegrity.report.service.ReportSectionService;
import com.interviewintegrity.report.web.dto.CreateReportSectionRequest;
import com.interviewintegrity.report.web.dto.ReportSectionResponse;
import com.interviewintegrity.report.web.dto.UpdateReportSectionRequest;
import com.interviewintegrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Report section endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/reports/{reportId}/sections")
@Tag(name = "Report Sections", description = "Manage report sections")
public final class ReportSectionController {

  private final ReportSectionService sectionService;
  private final ReportMapper mapper;

  /** Creates the controller bound to the report section service and mapper. */
  public ReportSectionController(ReportSectionService sectionService, ReportMapper mapper) {
    this.sectionService = sectionService;
    this.mapper = mapper;
  }

  /** Adds a section to a report. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Add a report section")
  public Mono<ReportSectionResponse> add(
      Authentication authentication,
      @PathVariable UUID reportId,
      @Valid @RequestBody CreateReportSectionRequest request) {
    return sectionService
        .addSection(
            reportId,
            SecurityPrincipals.organizationId(authentication),
            request.sectionType().trim(),
            request.title(),
            request.content(),
            request.orderIndex())
        .map(mapper::toResponse);
  }

  /** Lists the sections of a report in display order. */
  @GetMapping
  @Operation(summary = "List report sections")
  public Flux<ReportSectionResponse> list(
      Authentication authentication, @PathVariable UUID reportId) {
    return sectionService
        .listSections(reportId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Returns a single section. */
  @GetMapping("/{sectionId}")
  @Operation(summary = "Get a report section")
  public Mono<ReportSectionResponse> get(
      Authentication authentication, @PathVariable UUID sectionId) {
    return sectionService
        .getSection(sectionId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Updates a section. */
  @PatchMapping("/{sectionId}")
  @Operation(summary = "Update a report section")
  public Mono<ReportSectionResponse> update(
      Authentication authentication,
      @PathVariable UUID sectionId,
      @Valid @RequestBody UpdateReportSectionRequest request) {
    return sectionService
        .updateSection(
            sectionId,
            SecurityPrincipals.organizationId(authentication),
            request.title(),
            request.content(),
            request.orderIndex())
        .map(mapper::toResponse);
  }

  /** Removes a section from a report. */
  @DeleteMapping("/{sectionId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a report section")
  public Mono<Void> remove(Authentication authentication, @PathVariable UUID sectionId) {
    return sectionService.removeSection(
        sectionId, SecurityPrincipals.organizationId(authentication));
  }
}
