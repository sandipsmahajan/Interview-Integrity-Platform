package com.integrity.analytics.web;

import com.integrity.analytics.service.AnalyticsJobRunService;
import com.integrity.analytics.service.AnalyticsMapper;
import com.integrity.analytics.web.dto.CompleteJobRunRequest;
import com.integrity.analytics.web.dto.JobRunResponse;
import com.integrity.analytics.web.dto.StartJobRunRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

/** Analytics job run tracking endpoints. */
@RestController
@RequestMapping("/api/v1/analytics/job-runs")
@Tag(name = "Analytics Job Runs", description = "Track analytics aggregation runs")
public final class AnalyticsJobRunController {

  private final AnalyticsJobRunService jobRunService;
  private final AnalyticsMapper mapper;

  /** Creates the controller bound to the job run service and mapper. */
  public AnalyticsJobRunController(AnalyticsJobRunService jobRunService, AnalyticsMapper mapper) {
    this.jobRunService = jobRunService;
    this.mapper = mapper;
  }

  /** Starts a job run. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Start a job run")
  public Mono<JobRunResponse> start(@Valid @RequestBody StartJobRunRequest request) {
    return jobRunService.startJob(request.jobName().trim()).map(mapper::toResponse);
  }

  /** Marks a job run as succeeded. */
  @PostMapping("/{jobRunId}/succeed")
  @Operation(summary = "Succeed a job run")
  public Mono<JobRunResponse> succeed(
      @PathVariable Long jobRunId, @Valid @RequestBody CompleteJobRunRequest request) {
    return jobRunService.succeedJob(jobRunId, request.recordsProcessed()).map(mapper::toResponse);
  }

  /** Marks a job run as failed. */
  @PostMapping("/{jobRunId}/fail")
  @Operation(summary = "Fail a job run")
  public Mono<JobRunResponse> fail(
      @PathVariable Long jobRunId, @Valid @RequestBody CompleteJobRunRequest request) {
    return jobRunService.failJob(jobRunId, request.errorMessage()).map(mapper::toResponse);
  }

  /** Lists the most recent job runs. */
  @GetMapping
  @Operation(summary = "List job runs")
  public Flux<JobRunResponse> listRecent(@RequestParam(defaultValue = "20") int limit) {
    return jobRunService.listRecent(limit).map(mapper::toResponse);
  }
}
