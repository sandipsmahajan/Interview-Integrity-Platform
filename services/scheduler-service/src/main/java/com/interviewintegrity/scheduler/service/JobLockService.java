package com.interviewintegrity.scheduler.service;

import com.interviewintegrity.scheduler.domain.JobLock;
import com.interviewintegrity.scheduler.repository.JobLockRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import reactor.core.publisher.Mono;

/** Distributed locking over the job_locks table. */
public class JobLockService {

  private final JobLockRepository lockRepository;

  /** Wires the service with its repository. */
  public JobLockService(JobLockRepository lockRepository) {
    this.lockRepository = lockRepository;
  }

  /**
   * Attempts to acquire the lock for a job, empty when another owner holds it.
   *
   * @return the lock handle including the token needed to release it
   */
  public Mono<JobLock> tryAcquire(UUID jobId, String ownerId, Duration ttl) {
    String lockToken = UUID.randomUUID().toString();
    Instant expiresAt = Instant.now().plus(ttl);
    return lockRepository
        .tryAcquire(jobId, lockToken, ownerId, expiresAt)
        .flatMap(
            acquired ->
                acquired
                    ? Mono.just(new JobLock(jobId, lockToken, ownerId, expiresAt))
                    : Mono.empty());
  }

  /** Releases a lock, returning true when the caller still held it. */
  public Mono<Boolean> release(UUID jobId, String lockToken) {
    return lockRepository.release(jobId, lockToken);
  }
}
