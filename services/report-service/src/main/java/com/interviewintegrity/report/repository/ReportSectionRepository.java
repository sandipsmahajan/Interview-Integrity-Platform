package com.interviewintegrity.report.repository;

import com.interviewintegrity.report.domain.ReportSection;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link ReportSection} entities. */
public interface ReportSectionRepository extends ReactiveCrudRepository<ReportSection, UUID> {

  /** Finds a section by id within an organization. */
  @Query("SELECT * FROM report_sections WHERE id = :id AND organization_id = :organizationId")
  Mono<ReportSection> findByIdAndOrganization(UUID id, UUID organizationId);

  /** Lists the sections of a report in display order. */
  @Query(
      "SELECT * FROM report_sections WHERE report_id = :reportId "
          + "AND organization_id = :organizationId ORDER BY order_index")
  Flux<ReportSection> listByReportAndOrganization(UUID reportId, UUID organizationId);
}
