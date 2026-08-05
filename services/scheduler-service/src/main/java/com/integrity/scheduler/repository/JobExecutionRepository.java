package com.integrity.scheduler.repository;

import com.integrity.scheduler.domain.JobExecution;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link JobExecution} entities. */
public interface JobExecutionRepository extends ReactiveCrudRepository<JobExecution, UUID> {

  /** Finds an execution by id within an organization. */
  @Query("SELECT * FROM job_executions WHERE id = :id " + "AND organization_id = :organizationId")
  Mono<JobExecution> findByIdAndOrganization(UUID id, UUID organizationId);

  /** Lists the executions of a job, newest first. */
  @Query(
      "SELECT * FROM job_executions WHERE job_id = :jobId "
          + "AND organization_id = :organizationId ORDER BY started_at DESC")
  Flux<JobExecution> listByJob(UUID jobId, UUID organizationId);

  /** Lists the executions of an organization, newest first. */
  @Query(
      "SELECT * FROM job_executions WHERE organization_id = :organizationId "
          + "ORDER BY started_at DESC")
  Flux<JobExecution> listByOrganization(UUID organizationId);
}
