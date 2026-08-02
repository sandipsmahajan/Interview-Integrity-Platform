package com.interviewintegrity.scheduler.web;

import com.interviewintegrity.scheduler.service.JobExecutionService;
import com.interviewintegrity.scheduler.service.SchedulerMapper;
import com.interviewintegrity.scheduler.web.dto.FinishExecutionRequest;
import com.interviewintegrity.scheduler.web.dto.JobExecutionResponse;
import com.interviewintegrity.scheduler.web.dto.StartExecutionRequest;
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

/** Job execution endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/job-executions")
@Tag(name = "Job Executions", description = "Track job execution attempts")
public final class JobExecutionController {

  private final JobExecutionService executionService;
  private final SchedulerMapper mapper;

  /** Creates the controller bound to the execution service and mapper. */
  public JobExecutionController(JobExecutionService executionService, SchedulerMapper mapper) {
    this.executionService = executionService;
    this.mapper = mapper;
  }

  /** Starts an execution for a job. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Start a job execution")
  public Mono<JobExecutionResponse> start(
      Authentication authentication, @Valid @RequestBody StartExecutionRequest request) {
    return executionService
        .startExecution(
            SecurityPrincipals.organizationId(authentication),
            request.jobId(),
            request.workerId().trim())
        .map(mapper::toResponse);
  }

  /** Completes an execution successfully. */
  @PostMapping("/{executionId}/complete")
  @Operation(summary = "Complete a job execution")
  public Mono<JobExecutionResponse> complete(
      Authentication authentication,
      @PathVariable UUID executionId,
      @Valid @RequestBody FinishExecutionRequest request) {
    return executionService
        .completeExecution(
            executionId, SecurityPrincipals.organizationId(authentication), request.exitCode())
        .map(mapper::toResponse);
  }

  /** Fails an execution with an error detail. */
  @PostMapping("/{executionId}/fail")
  @Operation(summary = "Fail a job execution")
  public Mono<JobExecutionResponse> fail(
      Authentication authentication,
      @PathVariable UUID executionId,
      @Valid @RequestBody FinishExecutionRequest request) {
    return executionService
        .failExecution(
            executionId,
            SecurityPrincipals.organizationId(authentication),
            request.exitCode(),
            request.errorMessage())
        .map(mapper::toResponse);
  }

  /** Marks an execution as timed out. */
  @PostMapping("/{executionId}/timeout")
  @Operation(summary = "Mark a job execution as timed out")
  public Mono<JobExecutionResponse> timeout(
      Authentication authentication, @PathVariable UUID executionId) {
    return executionService
        .timeoutExecution(executionId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Marks an execution as skipped with a reason. */
  @PostMapping("/{executionId}/skip")
  @Operation(summary = "Mark a job execution as skipped")
  public Mono<JobExecutionResponse> skip(
      Authentication authentication,
      @PathVariable UUID executionId,
      @Valid @RequestBody FinishExecutionRequest request) {
    return executionService
        .skipExecution(
            executionId, SecurityPrincipals.organizationId(authentication), request.errorMessage())
        .map(mapper::toResponse);
  }

  /** Lists the executions of a job. */
  @GetMapping
  @Operation(summary = "List job executions")
  public Flux<JobExecutionResponse> list(Authentication authentication, @RequestParam UUID jobId) {
    return executionService
        .listByJob(jobId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }
}
