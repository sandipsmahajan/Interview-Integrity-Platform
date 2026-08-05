package com.integrity.scheduler.repository;

import com.integrity.scheduler.domain.JobStatus;
import com.integrity.scheduler.domain.ScheduledJob;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link ScheduledJob} entities. */
public interface ScheduledJobRepository extends ReactiveCrudRepository<ScheduledJob, UUID> {

  /** Finds a live job by id within an organization. */
  @Query(
      "SELECT * FROM scheduled_jobs WHERE id = :id "
          + "AND organization_id = :organizationId AND deleted_at IS NULL")
  Mono<ScheduledJob> findLiveByIdAndOrganization(UUID id, UUID organizationId);

  /** Lists the live jobs of an organization, name ordered. */
  @Query(
      "SELECT * FROM scheduled_jobs WHERE organization_id = :organizationId "
          + "AND deleted_at IS NULL ORDER BY name")
  Flux<ScheduledJob> listLiveByOrganization(UUID organizationId);

  /** Lists the live jobs of an organization in the given status. */
  @Query(
      "SELECT * FROM scheduled_jobs WHERE organization_id = :organizationId "
          + "AND status = :status AND deleted_at IS NULL ORDER BY name")
  Flux<ScheduledJob> listLiveByOrganizationAndStatus(UUID organizationId, JobStatus status);

  /** Lists the enabled live jobs whose next run is due, earliest first. */
  @Query(
      "SELECT * FROM scheduled_jobs WHERE organization_id = :organizationId "
          + "AND status = 'ENABLED' AND deleted_at IS NULL AND next_run_at IS NOT NULL "
          + "AND next_run_at <= :now ORDER BY next_run_at")
  Flux<ScheduledJob> listDueByOrganization(UUID organizationId, Instant now);
}
