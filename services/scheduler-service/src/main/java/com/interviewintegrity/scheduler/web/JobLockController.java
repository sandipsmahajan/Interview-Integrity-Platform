package com.interviewintegrity.scheduler.web;

import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.scheduler.domain.JobLock;
import com.interviewintegrity.scheduler.service.JobLockService;
import com.interviewintegrity.scheduler.web.dto.AcquireLockRequest;
import com.interviewintegrity.scheduler.web.dto.JobLockResponse;
import com.interviewintegrity.scheduler.web.dto.ReleaseLockRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Distributed lock endpoints. */
@RestController
@RequestMapping("/api/v1/job-locks")
@Tag(name = "Job Locks", description = "Distributed locks over scheduled jobs")
public final class JobLockController {

  private final JobLockService lockService;

  /** Creates the controller bound to the lock service. */
  public JobLockController(JobLockService lockService) {
    this.lockService = lockService;
  }

  /** Acquires the lock of a job, 404 when another owner holds it. */
  @PostMapping("/{jobId}/acquire")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Acquire a job lock")
  public Mono<JobLockResponse> acquire(
      @PathVariable UUID jobId, @Valid @RequestBody AcquireLockRequest request) {
    return lockService
        .tryAcquire(jobId, request.ownerId(), Duration.ofSeconds(request.ttlSeconds()))
        .switchIfEmpty(Mono.error(new ConflictException("Job lock is held by another owner")))
        .map(this::toResponse);
  }

  /** Releases a lock the caller holds. */
  @PostMapping("/{jobId}/release")
  @Operation(summary = "Release a job lock")
  public Mono<Void> release(
      @PathVariable UUID jobId, @Valid @RequestBody ReleaseLockRequest request) {
    return lockService.release(jobId, request.lockToken()).then();
  }

  private JobLockResponse toResponse(JobLock lock) {
    return new JobLockResponse(lock.jobId(), lock.lockToken(), lock.ownerId(), lock.expiresAt());
  }
}
