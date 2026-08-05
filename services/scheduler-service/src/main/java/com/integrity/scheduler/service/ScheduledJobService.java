package com.integrity.scheduler.service;

import com.integrity.exception.NotFoundException;
import com.integrity.scheduler.domain.JobStatus;
import com.integrity.scheduler.domain.ScheduledJob;
import com.integrity.scheduler.repository.JobLockRepository;
import com.integrity.scheduler.repository.ScheduledJobRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages the scheduled job definitions of an organization. */
public class ScheduledJobService {

  private final ScheduledJobRepository jobRepository;
  private final JobLockRepository lockRepository;

  /** Wires the service with its repositories. */
  public ScheduledJobService(
      ScheduledJobRepository jobRepository, JobLockRepository lockRepository) {
    this.jobRepository = jobRepository;
    this.lockRepository = lockRepository;
  }

  /** Creates a new scheduled job. */
  @Transactional
  public Mono<ScheduledJob> createJob(
      UUID organizationId,
      String name,
      String jobType,
      String cronExpression,
      String handler,
      String payload,
      int maxRetries,
      int timeoutSeconds,
      UUID createdBy) {
    return jobRepository.save(
        new ScheduledJob(
            organizationId,
            name,
            jobType,
            cronExpression,
            handler,
            payload,
            maxRetries,
            timeoutSeconds,
            createdBy));
  }

  /** Returns a single live job of the organization. */
  @Transactional(readOnly = true)
  public Mono<ScheduledJob> getJob(UUID jobId, UUID organizationId) {
    return jobRepository
        .findLiveByIdAndOrganization(jobId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Scheduled job not found")));
  }

  /** Lists the live jobs of the organization, optionally filtered by status. */
  @Transactional(readOnly = true)
  public Flux<ScheduledJob> listJobs(UUID organizationId, JobStatus status) {
    if (status == null) {
      return jobRepository.listLiveByOrganization(organizationId);
    }
    return jobRepository.listLiveByOrganizationAndStatus(organizationId, status);
  }

  /** Updates a scheduled job definition. */
  @Transactional
  public Mono<ScheduledJob> updateJob(
      UUID jobId,
      UUID organizationId,
      String name,
      String cronExpression,
      String payload,
      int maxRetries,
      int timeoutSeconds,
      UUID updatedBy) {
    return getJob(jobId, organizationId)
        .map(
            job -> {
              job.update(name, cronExpression, payload, maxRetries, timeoutSeconds, updatedBy);
              return job;
            })
        .flatMap(jobRepository::save);
  }

  /** Pauses a job. */
  @Transactional
  public Mono<ScheduledJob> pauseJob(UUID jobId, UUID organizationId, UUID updatedBy) {
    return getJob(jobId, organizationId)
        .map(
            job -> {
              job.pause(updatedBy);
              return job;
            })
        .flatMap(jobRepository::save);
  }

  /** Resumes a paused job. */
  @Transactional
  public Mono<ScheduledJob> resumeJob(UUID jobId, UUID organizationId, UUID updatedBy) {
    return getJob(jobId, organizationId)
        .map(
            job -> {
              job.resume(updatedBy);
              return job;
            })
        .flatMap(jobRepository::save);
  }

  /** Disables a job. */
  @Transactional
  public Mono<ScheduledJob> disableJob(UUID jobId, UUID organizationId, UUID updatedBy) {
    return getJob(jobId, organizationId)
        .map(
            job -> {
              job.disable(updatedBy);
              return job;
            })
        .flatMap(jobRepository::save);
  }

  /** Enables a disabled job. */
  @Transactional
  public Mono<ScheduledJob> enableJob(UUID jobId, UUID organizationId, UUID updatedBy) {
    return getJob(jobId, organizationId)
        .map(
            job -> {
              job.enable(updatedBy);
              return job;
            })
        .flatMap(jobRepository::save);
  }

  /** Soft deletes a job. */
  @Transactional
  public Mono<Void> deleteJob(UUID jobId, UUID organizationId, UUID deletedBy) {
    return getJob(jobId, organizationId)
        .flatMap(
            job -> {
              job.delete(deletedBy);
              return jobRepository.save(job).then();
            });
  }

  /** Lists the enabled jobs of the organization whose next run is due. */
  @Transactional(readOnly = true)
  public Flux<ScheduledJob> listDue(UUID organizationId) {
    return jobRepository.listDueByOrganization(organizationId, Instant.now());
  }

  /**
   * Runs the due jobs of the organization: each job is advanced under a distributed lock so only
   * one worker executes it at a time.
   */
  @Transactional
  public Flux<ScheduledJob> runDue(UUID organizationId) {
    Instant now = Instant.now();
    String ownerId = "scheduler-service";
    return jobRepository
        .listDueByOrganization(organizationId, now)
        .concatMap(job -> runDueJob(job, ownerId, now));
  }

  private Mono<ScheduledJob> runDueJob(ScheduledJob job, String ownerId, Instant now) {
    String lockToken = UUID.randomUUID().toString();
    return lockRepository
        .tryAcquire(job.getId(), lockToken, ownerId, now.plusSeconds(300))
        .flatMap(
            acquired -> {
              if (!acquired) {
                return Mono.just(job);
              }
              job.advance(null);
              return jobRepository
                  .save(job)
                  .flatMap(
                      saved -> lockRepository.release(job.getId(), lockToken).thenReturn(saved));
            });
  }
}
