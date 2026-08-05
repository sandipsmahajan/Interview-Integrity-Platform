package com.integrity.analytics.repository;

import com.integrity.analytics.domain.DailyCandidateSummary;
import io.r2dbc.spi.Row;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Database client backed repository for the {@code daily_candidate_summaries} table.
 *
 * <p>The table uses a composite primary key (summary_date, organization_id, candidate_id), which
 * Spring Data R2DBC entities cannot map directly, so explicit SQL is used for all operations.
 */
public final class CandidateSummaryRepository {

  private final DatabaseClient databaseClient;

  /** Creates a repository bound to the given database client. */
  public CandidateSummaryRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  /** Upserts a daily candidate summary (idempotent per primary key). */
  public Mono<DailyCandidateSummary> upsert(DailyCandidateSummary summary) {
    DatabaseClient.GenericExecuteSpec spec =
        databaseClient
            .sql(
                "INSERT INTO daily_candidate_summaries "
                    + "(summary_date, organization_id, candidate_id, interviews_attended, "
                    + " avg_score, assessments_completed, violations, created_at, updated_at) "
                    + "VALUES (:summaryDate, :organizationId, :candidateId, :attended, :avgScore, "
                    + " :assessments, :violations, now(), now()) "
                    + "ON CONFLICT (summary_date, organization_id, candidate_id) DO UPDATE SET "
                    + " interviews_attended = EXCLUDED.interviews_attended, "
                    + " avg_score = EXCLUDED.avg_score, "
                    + " assessments_completed = EXCLUDED.assessments_completed, "
                    + " violations = EXCLUDED.violations, "
                    + " updated_at = now()")
            .bind("summaryDate", summary.getSummaryDate())
            .bind("organizationId", summary.getOrganizationId())
            .bind("candidateId", summary.getCandidateId())
            .bind("attended", summary.getInterviewsAttended())
            .bind("assessments", summary.getAssessmentsCompleted())
            .bind("violations", summary.getViolations());
    if (summary.getAvgScore() != null) {
      spec = spec.bind("avgScore", summary.getAvgScore());
    } else {
      spec = spec.bindNull("avgScore", BigDecimal.class);
    }
    return spec.then().thenReturn(summary);
  }

  /** Finds the summary for a candidate on a specific date. */
  public Mono<DailyCandidateSummary> find(UUID organizationId, UUID candidateId, LocalDate date) {
    return databaseClient
        .sql(
            "SELECT * FROM daily_candidate_summaries "
                + "WHERE organization_id = :organizationId AND candidate_id = :candidateId "
                + "AND summary_date = :summaryDate")
        .bind("organizationId", organizationId)
        .bind("candidateId", candidateId)
        .bind("summaryDate", date)
        .map((row, metadata) -> map(row))
        .one();
  }

  /** Lists the summaries of a candidate within a date range, oldest first. */
  public Flux<DailyCandidateSummary> list(
      UUID organizationId, UUID candidateId, LocalDate from, LocalDate to) {
    return databaseClient
        .sql(
            "SELECT * FROM daily_candidate_summaries "
                + "WHERE organization_id = :organizationId AND candidate_id = :candidateId "
                + "AND summary_date BETWEEN :from AND :to ORDER BY summary_date")
        .bind("organizationId", organizationId)
        .bind("candidateId", candidateId)
        .bind("from", from)
        .bind("to", to)
        .map((row, metadata) -> map(row))
        .all();
  }

  private DailyCandidateSummary map(Row row) {
    return new DailyCandidateSummary(
        row.get("summary_date", LocalDate.class),
        row.get("organization_id", UUID.class),
        row.get("candidate_id", UUID.class),
        toLong(row.get("interviews_attended", Long.class)),
        row.get("avg_score", BigDecimal.class),
        toLong(row.get("assessments_completed", Long.class)),
        toLong(row.get("violations", Long.class)),
        row.get("created_at", Instant.class),
        row.get("updated_at", Instant.class));
  }

  private static long toLong(Long value) {
    return value == null ? 0L : value;
  }
}
