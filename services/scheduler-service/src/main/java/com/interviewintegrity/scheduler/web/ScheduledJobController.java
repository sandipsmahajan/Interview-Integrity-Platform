package com.interviewintegrity.scheduler.web;

import com.interviewintegrity.scheduler.domain.JobStatus;
import com.interviewintegrity.scheduler.service.ScheduledJobService;
import com.interviewintegrity.scheduler.service.SchedulerMapper;
import com.interviewintegrity.scheduler.web.dto.CreateScheduledJobRequest;
import com.interviewintegrity.scheduler.web.dto.ScheduledJobResponse;
import com.interviewintegrity.scheduler.web.dto.UpdateScheduledJobRequest;
import com.interviewintegrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Scheduled job endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/scheduled-jobs")
@Tag(name = "Scheduled Jobs", description = "Manage scheduled job definitions")
public final class ScheduledJobController {

  private final ScheduledJobService jobService;
  private final SchedulerMapper mapper;

  /** Creates the controller bound to the job service and mapper. */
  public ScheduledJobController(ScheduledJobService jobService, SchedulerMapper mapper) {
    this.jobService = jobService;
    this.mapper = mapper;
  }

  /** Creates a scheduled job. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a scheduled job")
  public Mono<ScheduledJobResponse> create(
      Authentication authentication, @Valid @RequestBody CreateScheduledJobRequest request) {
    return jobService
        .createJob(
            SecurityPrincipals.organizationId(authentication),
            request.name().trim(),
            request.jobType().trim(),
            request.cronExpression(),
            request.handler().trim(),
            request.payload(),
            request.maxRetries(),
            request.timeoutSeconds(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Lists the jobs of the organization, optionally filtered by status. */
  @GetMapping
  @Operation(summary = "List scheduled jobs")
  public Flux<ScheduledJobResponse> list(
      Authentication authentication, @RequestParam(required = false) JobStatus status) {
    return jobService
        .listJobs(SecurityPrincipals.organizationId(authentication), status)
        .map(mapper::toResponse);
  }

  /** Returns a single scheduled job. */
  @GetMapping("/{jobId}")
  @Operation(summary = "Get a scheduled job")
  public Mono<ScheduledJobResponse> get(Authentication authentication, @PathVariable UUID jobId) {
    return jobService
        .getJob(jobId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Updates a scheduled job. */
  @PutMapping("/{jobId}")
  @Operation(summary = "Update a scheduled job")
  public Mono<ScheduledJobResponse> update(
      Authentication authentication,
      @PathVariable UUID jobId,
      @Valid @RequestBody UpdateScheduledJobRequest request) {
    return jobService
        .updateJob(
            jobId,
            SecurityPrincipals.organizationId(authentication),
            request.name().trim(),
            request.cronExpression(),
            request.payload(),
            request.maxRetries(),
            request.timeoutSeconds(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Pauses a scheduled job. */
  @PostMapping("/{jobId}/pause")
  @Operation(summary = "Pause a scheduled job")
  public Mono<ScheduledJobResponse> pause(Authentication authentication, @PathVariable UUID jobId) {
    return jobService
        .pauseJob(
            jobId,
            SecurityPrincipals.organizationId(authentication),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Resumes a paused scheduled job. */
  @PostMapping("/{jobId}/resume")
  @Operation(summary = "Resume a scheduled job")
  public Mono<ScheduledJobResponse> resume(
      Authentication authentication, @PathVariable UUID jobId) {
    return jobService
        .resumeJob(
            jobId,
            SecurityPrincipals.organizationId(authentication),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Disables a scheduled job. */
  @PostMapping("/{jobId}/disable")
  @Operation(summary = "Disable a scheduled job")
  public Mono<ScheduledJobResponse> disable(
      Authentication authentication, @PathVariable UUID jobId) {
    return jobService
        .disableJob(
            jobId,
            SecurityPrincipals.organizationId(authentication),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Enables a disabled scheduled job. */
  @PostMapping("/{jobId}/enable")
  @Operation(summary = "Enable a scheduled job")
  public Mono<ScheduledJobResponse> enable(
      Authentication authentication, @PathVariable UUID jobId) {
    return jobService
        .enableJob(
            jobId,
            SecurityPrincipals.organizationId(authentication),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Lists the enabled jobs of the organization whose next run is due. */
  @GetMapping("/due")
  @Operation(summary = "List due scheduled jobs")
  public Flux<ScheduledJobResponse> due(Authentication authentication) {
    return jobService
        .listDue(SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Advances the due jobs of the organization under distributed locks. */
  @PostMapping("/run-due")
  @Operation(summary = "Run due scheduled jobs")
  public Flux<ScheduledJobResponse> runDue(Authentication authentication) {
    return jobService
        .runDue(SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Soft deletes a scheduled job. */
  @DeleteMapping("/{jobId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a scheduled job")
  public Mono<Void> delete(Authentication authentication, @PathVariable UUID jobId) {
    return jobService.deleteJob(
        jobId,
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication));
  }
}
