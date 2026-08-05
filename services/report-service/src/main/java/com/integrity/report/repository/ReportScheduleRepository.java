package com.integrity.report.repository;

import com.integrity.report.domain.ReportSchedule;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link ReportSchedule} entities. */
public interface ReportScheduleRepository extends ReactiveCrudRepository<ReportSchedule, UUID> {

  /** Finds a live schedule by id. */
  @Query("SELECT * FROM report_schedules WHERE id = :id AND deleted_at IS NULL")
  Mono<ReportSchedule> findLiveById(UUID id);

  /** Lists the live schedules of an organization, newest first. */
  @Query(
      "SELECT * FROM report_schedules WHERE organization_id = :organizationId "
          + "AND deleted_at IS NULL ORDER BY created_at DESC")
  Flux<ReportSchedule> listLiveByOrganization(UUID organizationId);
}
