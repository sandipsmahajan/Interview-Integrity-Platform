package com.interviewintegrity.report.repository;

import com.interviewintegrity.report.domain.Report;
import com.interviewintegrity.report.domain.ReportStatus;
import com.interviewintegrity.report.domain.ReportType;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link Report} entities. */
public interface ReportRepository extends ReactiveCrudRepository<Report, UUID> {

  /** Finds a report by id within an organization. */
  @Query("SELECT * FROM reports WHERE id = :id AND organization_id = :organizationId")
  Mono<Report> findByIdAndOrganization(UUID id, UUID organizationId);

  /** Lists the reports of an organization, newest first. */
  @Query(
      "SELECT * FROM reports WHERE organization_id = :organizationId "
          + "ORDER BY requested_at DESC")
  Flux<Report> listByOrganization(UUID organizationId);

  /** Lists the reports of an organization in the given status, newest first. */
  @Query(
      "SELECT * FROM reports WHERE organization_id = :organizationId AND status = :status "
          + "ORDER BY requested_at DESC")
  Flux<Report> listByOrganizationAndStatus(UUID organizationId, ReportStatus status);

  /** Lists the reports of an organization of the given type, newest first. */
  @Query(
      "SELECT * FROM reports WHERE organization_id = :organizationId AND type = :type "
          + "ORDER BY requested_at DESC")
  Flux<Report> listByOrganizationAndType(UUID organizationId, ReportType type);
}
