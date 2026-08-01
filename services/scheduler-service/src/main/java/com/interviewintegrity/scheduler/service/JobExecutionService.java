package com.interviewintegrity.scheduler.service;

import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.scheduler.domain.JobExecution;
import com.interviewintegrity.scheduler.repository.JobExecutionRepository;
import com.interviewintegrity.scheduler.repository.ScheduledJobRepository;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Tracks the execution attempts of the scheduled jobs of an organization. */
public class JobExecutionService {

  private final JobExecutionRepository executionRepository;
  private final ScheduledJobRepository jobRepository;

  /** Wires the service with its repositories. */
  public JobExecutionService(
      JobExecutionRepository executionRepository, ScheduledJobRepository jobRepository) {
    this.executionRepository = executionRepository;
    this.jobRepository = jobRepository;
  }

  /** Starts a new execution for a job on a worker. */
  @Transactional
  public Mono<JobExecution> startExecution(UUID organizationId, UUID jobId, String workerId) {
    return jobRepository
        .findLiveByIdAndOrganization(jobId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Scheduled job not found")))
        .flatMap(
            ignored -> executionRepository.save(new JobExecution(organizationId, jobId, workerId)));
  }

  /** Completes an execution successfully. */
  @Transactional
  public Mono<JobExecution> completeExecution(UUID executionId, UUID organizationId, int exitCode) {
    return getExecution(executionId, organizationId)
        .map(
            execution -> {
              execution.succeed(exitCode);
              return execution;
            })
        .flatMap(executionRepository::save);
  }

  /** Fails an execution with an error detail. */
  @Transactional
  public Mono<JobExecution> failExecution(
      UUID executionId, UUID organizationId, int exitCode, String errorMessage) {
    return getExecution(executionId, organizationId)
        .map(
            execution -> {
              execution.fail(exitCode, errorMessage);
              return execution;
            })
        .flatMap(executionRepository::save);
  }

  /** Marks an execution as timed out. */
  @Transactional
  public Mono<JobExecution> timeoutExecution(UUID executionId, UUID organizationId) {
    return getExecution(executionId, organizationId)
        .map(
            execution -> {
              execution.timeOut();
              return execution;
            })
        .flatMap(executionRepository::save);
  }

  /** Marks an execution as skipped with a reason. */
  @Transactional
  public Mono<JobExecution> skipExecution(UUID executionId, UUID organizationId, String reason) {
    return getExecution(executionId, organizationId)
        .map(
            execution -> {
              execution.skip(reason);
              return execution;
            })
        .flatMap(executionRepository::save);
  }

  /** Lists the executions of a job. */
  @Transactional(readOnly = true)
  public Flux<JobExecution> listByJob(UUID jobId, UUID organizationId) {
    return jobRepository
        .findLiveByIdAndOrganization(jobId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Scheduled job not found")))
        .flatMapMany(ignored -> executionRepository.listByJob(jobId, organizationId));
  }

  private Mono<JobExecution> getExecution(UUID executionId, UUID organizationId) {
    return executionRepository
        .findByIdAndOrganization(executionId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Job execution not found")));
  }
}
