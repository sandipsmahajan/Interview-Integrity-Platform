package com.interviewintegrity.report.web;

import com.interviewintegrity.report.domain.ReportSchedule;
import com.interviewintegrity.report.service.ReportScheduleService;
import com.interviewintegrity.report.web.dto.CreateReportScheduleRequest;
import com.interviewintegrity.report.web.dto.ReportScheduleResponse;
import com.interviewintegrity.report.web.dto.UpdateReportScheduleRequest;
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

/** Report schedule endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/report-schedules")
@Tag(name = "Report Schedules", description = "Manage recurring report schedules")
public final class ReportScheduleController {

  private final ReportScheduleService scheduleService;

  /** Creates the controller bound to the report schedule service. */
  public ReportScheduleController(ReportScheduleService scheduleService) {
    this.scheduleService = scheduleService;
  }

  /** Creates a recurring report schedule. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a report schedule")
  public Mono<ReportScheduleResponse> create(
      Authentication authentication, @Valid @RequestBody CreateReportScheduleRequest request) {
    return scheduleService
        .createSchedule(
            SecurityPrincipals.organizationId(authentication),
            request.type(),
            request.cronExpression().trim(),
            request.format(),
            request.recipients(),
            request.parameters(),
            request.nextRunAt(),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  /** Lists the schedules of the organization. */
  @GetMapping
  @Operation(summary = "List report schedules")
  public Flux<ReportScheduleResponse> list(Authentication authentication) {
    return scheduleService
        .listSchedules(SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  /** Returns a single schedule. */
  @GetMapping("/{scheduleId}")
  @Operation(summary = "Get a report schedule")
  public Mono<ReportScheduleResponse> get(
      Authentication authentication, @PathVariable UUID scheduleId) {
    return scheduleService
        .getSchedule(scheduleId, SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  /** Updates a schedule. */
  @PatchMapping("/{scheduleId}")
  @Operation(summary = "Update a report schedule")
  public Mono<ReportScheduleResponse> update(
      Authentication authentication,
      @PathVariable UUID scheduleId,
      @Valid @RequestBody UpdateReportScheduleRequest request) {
    return scheduleService
        .updateSchedule(
            scheduleId,
            SecurityPrincipals.organizationId(authentication),
            request.cronExpression().trim(),
            request.format(),
            request.recipients(),
            request.parameters(),
            request.nextRunAt(),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  /** Enables a schedule. */
  @PostMapping("/{scheduleId}/enable")
  @Operation(summary = "Enable a report schedule")
  public Mono<ReportScheduleResponse> enable(
      Authentication authentication, @PathVariable UUID scheduleId) {
    return scheduleService
        .enableSchedule(
            scheduleId,
            SecurityPrincipals.organizationId(authentication),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  /** Disables a schedule. */
  @PostMapping("/{scheduleId}/disable")
  @Operation(summary = "Disable a report schedule")
  public Mono<ReportScheduleResponse> disable(
      Authentication authentication, @PathVariable UUID scheduleId) {
    return scheduleService
        .disableSchedule(
            scheduleId,
            SecurityPrincipals.organizationId(authentication),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  /** Soft deletes a schedule. */
  @DeleteMapping("/{scheduleId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a report schedule")
  public Mono<Void> delete(Authentication authentication, @PathVariable UUID scheduleId) {
    return scheduleService.deleteSchedule(
        scheduleId,
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication));
  }

  private ReportScheduleResponse toResponse(ReportSchedule schedule) {
    return new ReportScheduleResponse(
        schedule.getId(),
        schedule.getOrganizationId(),
        schedule.getType(),
        schedule.getCronExpression(),
        schedule.getFormat(),
        schedule.getRecipients(),
        schedule.getParameters(),
        schedule.isEnabled(),
        schedule.getNextRunAt(),
        schedule.getLastRunAt(),
        schedule.getCreatedAt(),
        schedule.getUpdatedAt());
  }
}
