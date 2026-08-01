package com.interviewintegrity.analytics.repository;

import com.interviewintegrity.analytics.domain.DailyOrganizationSummary;
import io.r2dbc.spi.Row;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Database client backed repository for the {@code daily_organization_summaries} table.
 *
 * <p>The table uses a composite primary key (summary_date, organization_id), which Spring Data
 * R2DBC entities cannot map directly, so explicit SQL is used for all operations.
 */
public final class OrganizationSummaryRepository {

  private final DatabaseClient databaseClient;

  /** Creates a repository bound to the given database client. */
  public OrganizationSummaryRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  /** Upserts a daily organization summary (idempotent per primary key). */
  public Mono<DailyOrganizationSummary> upsert(DailyOrganizationSummary summary) {
    DatabaseClient.GenericExecuteSpec spec =
        databaseClient
            .sql(
                "INSERT INTO daily_organization_summaries "
                    + "(summary_date, organization_id, interviews_scheduled, interviews_completed, "
                    + " interviews_cancelled, candidates_active, recruiters_active, violations, "
                    + " avg_integrity_score, created_at, updated_at) "
                    + "VALUES (:summaryDate, :organizationId, :scheduled, :completed, :cancelled, "
                    + " :activeCandidates, :activeRecruiters, :violations, :avgScore, now(), now()) "
                    + "ON CONFLICT (summary_date, organization_id) DO UPDATE SET "
                    + " interviews_scheduled = EXCLUDED.interviews_scheduled, "
                    + " interviews_completed = EXCLUDED.interviews_completed, "
                    + " interviews_cancelled = EXCLUDED.interviews_cancelled, "
                    + " candidates_active = EXCLUDED.candidates_active, "
                    + " recruiters_active = EXCLUDED.recruiters_active, "
                    + " violations = EXCLUDED.violations, "
                    + " avg_integrity_score = EXCLUDED.avg_integrity_score, "
                    + " updated_at = now()")
            .bind("summaryDate", summary.getSummaryDate())
            .bind("organizationId", summary.getOrganizationId())
            .bind("scheduled", summary.getInterviewsScheduled())
            .bind("completed", summary.getInterviewsCompleted())
            .bind("cancelled", summary.getInterviewsCancelled())
            .bind("activeCandidates", summary.getCandidatesActive())
            .bind("activeRecruiters", summary.getRecruitersActive())
            .bind("violations", summary.getViolations());
    if (summary.getAvgIntegrityScore() != null) {
      spec = spec.bind("avgScore", summary.getAvgIntegrityScore());
    } else {
      spec = spec.bindNull("avgScore", BigDecimal.class);
    }
    return spec.then().thenReturn(summary);
  }

  /** Finds the summary for an organization on a specific date. */
  public Mono<DailyOrganizationSummary> find(UUID organizationId, LocalDate date) {
    return databaseClient
        .sql(
            "SELECT * FROM daily_organization_summaries "
                + "WHERE organization_id = :organizationId AND summary_date = :summaryDate")
        .bind("organizationId", organizationId)
        .bind("summaryDate", date)
        .map((row, metadata) -> map(row))
        .one();
  }

  /** Lists the summaries of an organization within a date range, oldest first. */
  public Flux<DailyOrganizationSummary> list(UUID organizationId, LocalDate from, LocalDate to) {
    return databaseClient
        .sql(
            "SELECT * FROM daily_organization_summaries "
                + "WHERE organization_id = :organizationId AND summary_date BETWEEN :from AND :to "
                + "ORDER BY summary_date")
        .bind("organizationId", organizationId)
        .bind("from", from)
        .bind("to", to)
        .map((row, metadata) -> map(row))
        .all();
  }

  /** Refreshes the monthly materialized views. */
  public Mono<Void> refreshMonthlyViews() {
    return databaseClient.sql("SELECT analytics_refresh_monthly_views()").then();
  }

  private DailyOrganizationSummary map(Row row) {
    return new DailyOrganizationSummary(
        row.get("summary_date", LocalDate.class),
        row.get("organization_id", UUID.class),
        toLong(row.get("interviews_scheduled", Long.class)),
        toLong(row.get("interviews_completed", Long.class)),
        toLong(row.get("interviews_cancelled", Long.class)),
        toLong(row.get("candidates_active", Long.class)),
        toLong(row.get("recruiters_active", Long.class)),
        toLong(row.get("violations", Long.class)),
        row.get("avg_integrity_score", BigDecimal.class),
        row.get("created_at", Instant.class),
        row.get("updated_at", Instant.class));
  }

  private static long toLong(Long value) {
    return value == null ? 0L : value;
  }
}
