package com.integrity.scheduler.repository;

import java.time.Instant;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

/**
 * Database client backed repository for the {@code job_locks} distributed lock table.
 *
 * <p>Acquisition is an insert with an expiry window; only the owner holding the lock token can
 * release it.
 */
public final class JobLockRepository {

  private final DatabaseClient databaseClient;

  /** Creates a repository bound to the given database client. */
  public JobLockRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  /** Attempts to acquire the lock for a job, returning true when this owner won. */
  public Mono<Boolean> tryAcquire(UUID jobId, String lockToken, String ownerId, Instant expiresAt) {
    return databaseClient
        .sql(
            "INSERT INTO job_locks (job_id, lock_token, owner_id, acquired_at, expires_at) "
                + "VALUES (:jobId, :lockToken, :ownerId, now(), :expiresAt) "
                + "ON CONFLICT (job_id) DO NOTHING RETURNING job_id")
        .bind("jobId", jobId)
        .bind("lockToken", lockToken)
        .bind("ownerId", ownerId)
        .bind("expiresAt", expiresAt)
        .map((row, metadata) -> row.get(0, UUID.class))
        .one()
        .hasElement();
  }

  /** Releases the lock for a job when the caller still holds it. */
  public Mono<Boolean> release(UUID jobId, String lockToken) {
    return databaseClient
        .sql(
            "DELETE FROM job_locks WHERE job_id = :jobId AND lock_token = :lockToken "
                + "RETURNING job_id")
        .bind("jobId", jobId)
        .bind("lockToken", lockToken)
        .map((row, metadata) -> row.get(0, UUID.class))
        .one()
        .hasElement();
  }
}
