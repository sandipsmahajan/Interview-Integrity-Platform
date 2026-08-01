package com.interviewintegrity.analytics.repository;

import com.interviewintegrity.analytics.domain.DailyInterviewSummary;
import io.r2dbc.spi.Row;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Database client backed repository for the {@code daily_interview_summaries} table.
 *
 * <p>The table uses a composite primary key (summary_date, organization_id, interview_id), which
 * Spring Data R2DBC entities cannot map directly, so explicit SQL is used for all operations.
 */
public final class InterviewSummaryRepository {

  private final DatabaseClient databaseClient;

  /** Creates a repository bound to the given database client. */
  public InterviewSummaryRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  /** Upserts a daily interview summary (idempotent per primary key). */
  public Mono<DailyInterviewSummary> upsert(DailyInterviewSummary summary) {
    DatabaseClient.GenericExecuteSpec spec =
        databaseClient
            .sql(
                "INSERT INTO daily_interview_summaries "
                    + "(summary_date, organization_id, interview_id, duration_minutes, "
                    + " integrity_score, violations, status, created_at, updated_at) "
                    + "VALUES (:summaryDate, :organizationId, :interviewId, :duration, "
                    + " :integrityScore, :violations, :status, now(), now()) "
                    + "ON CONFLICT (summary_date, organization_id, interview_id) DO UPDATE SET "
                    + " duration_minutes = EXCLUDED.duration_minutes, "
                    + " integrity_score = EXCLUDED.integrity_score, "
                    + " violations = EXCLUDED.violations, "
                    + " status = EXCLUDED.status, "
                    + " updated_at = now()")
            .bind("summaryDate", summary.getSummaryDate())
            .bind("organizationId", summary.getOrganizationId())
            .bind("interviewId", summary.getInterviewId())
            .bind("violations", summary.getViolations())
            .bind("status", summary.getStatus());
    if (summary.getDurationMinutes() != null) {
      spec = spec.bind("duration", summary.getDurationMinutes());
    } else {
      spec = spec.bindNull("duration", Integer.class);
    }
    if (summary.getIntegrityScore() != null) {
      spec = spec.bind("integrityScore", summary.getIntegrityScore());
    } else {
      spec = spec.bindNull("integrityScore", BigDecimal.class);
    }
    return spec.then().thenReturn(summary);
  }

  /** Finds the summary for an interview on a specific date. */
  public Mono<DailyInterviewSummary> find(UUID organizationId, UUID interviewId, LocalDate date) {
    return databaseClient
        .sql(
            "SELECT * FROM daily_interview_summaries "
                + "WHERE organization_id = :organizationId AND interview_id = :interviewId "
                + "AND summary_date = :summaryDate")
        .bind("organizationId", organizationId)
        .bind("interviewId", interviewId)
        .bind("summaryDate", date)
        .map((row, metadata) -> map(row))
        .one();
  }

  /** Lists the summaries of an interview within a date range, oldest first. */
  public Flux<DailyInterviewSummary> list(
      UUID organizationId, UUID interviewId, LocalDate from, LocalDate to) {
    return databaseClient
        .sql(
            "SELECT * FROM daily_interview_summaries "
                + "WHERE organization_id = :organizationId AND interview_id = :interviewId "
                + "AND summary_date BETWEEN :from AND :to ORDER BY summary_date")
        .bind("organizationId", organizationId)
        .bind("interviewId", interviewId)
        .bind("from", from)
        .bind("to", to)
        .map((row, metadata) -> map(row))
        .all();
  }

  private DailyInterviewSummary map(Row row) {
    return new DailyInterviewSummary(
        row.get("summary_date", LocalDate.class),
        row.get("organization_id", UUID.class),
        row.get("interview_id", UUID.class),
        row.get("duration_minutes", Integer.class),
        row.get("integrity_score", BigDecimal.class),
        toLong(row.get("violations", Long.class)),
        row.get("status", String.class),
        row.get("created_at", Instant.class),
        row.get("updated_at", Instant.class));
  }

  private static long toLong(Long value) {
    return value == null ? 0L : value;
  }
}
