package com.integrity.report.repository;

import com.integrity.report.domain.ReportRequest;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link ReportRequest} entities. */
public interface ReportRequestRepository extends ReactiveCrudRepository<ReportRequest, UUID> {

  /** Finds a request by id within an organization. */
  @Query("SELECT * FROM report_requests WHERE id = :id AND organization_id = :organizationId")
  Mono<ReportRequest> findByIdAndOrganization(UUID id, UUID organizationId);

  /** Lists the requests for a report, newest first. */
  @Query(
      "SELECT * FROM report_requests WHERE report_id = :reportId "
          + "AND organization_id = :organizationId ORDER BY requested_at DESC")
  Flux<ReportRequest> listByReportAndOrganization(UUID reportId, UUID organizationId);
}
