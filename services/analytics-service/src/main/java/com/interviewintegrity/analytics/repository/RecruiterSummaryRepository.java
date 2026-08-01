package com.interviewintegrity.analytics.repository;

import com.interviewintegrity.analytics.domain.DailyRecruiterSummary;
import io.r2dbc.spi.Row;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Database client backed repository for the {@code daily_recruiter_summaries} table.
 *
 * <p>The table uses a composite primary key (summary_date, organization_id, recruiter_id), which
 * Spring Data R2DBC entities cannot map directly, so explicit SQL is used for all operations.
 */
public final class RecruiterSummaryRepository {

  private final DatabaseClient databaseClient;

  /** Creates a repository bound to the given database client. */
  public RecruiterSummaryRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  /** Upserts a daily recruiter summary (idempotent per primary key). */
  public Mono<DailyRecruiterSummary> upsert(DailyRecruiterSummary summary) {
    DatabaseClient.GenericExecuteSpec spec =
        databaseClient
            .sql(
                "INSERT INTO daily_recruiter_summaries "
                    + "(summary_date, organization_id, recruiter_id, interviews_held, "
                    + " interviews_completed, candidates_contacted, avg_feedback_rating, "
                    + " violations, created_at, updated_at) "
                    + "VALUES (:summaryDate, :organizationId, :recruiterId, :held, :completed, "
                    + " :contacted, :feedbackRating, :violations, now(), now()) "
                    + "ON CONFLICT (summary_date, organization_id, recruiter_id) DO UPDATE SET "
                    + " interviews_held = EXCLUDED.interviews_held, "
                    + " interviews_completed = EXCLUDED.interviews_completed, "
                    + " candidates_contacted = EXCLUDED.candidates_contacted, "
                    + " avg_feedback_rating = EXCLUDED.avg_feedback_rating, "
                    + " violations = EXCLUDED.violations, "
                    + " updated_at = now()")
            .bind("summaryDate", summary.getSummaryDate())
            .bind("organizationId", summary.getOrganizationId())
            .bind("recruiterId", summary.getRecruiterId())
            .bind("held", summary.getInterviewsHeld())
            .bind("completed", summary.getInterviewsCompleted())
            .bind("contacted", summary.getCandidatesContacted())
            .bind("violations", summary.getViolations());
    if (summary.getAvgFeedbackRating() != null) {
      spec = spec.bind("feedbackRating", summary.getAvgFeedbackRating());
    } else {
      spec = spec.bindNull("feedbackRating", BigDecimal.class);
    }
    return spec.then().thenReturn(summary);
  }

  /** Finds the summary for a recruiter on a specific date. */
  public Mono<DailyRecruiterSummary> find(UUID organizationId, UUID recruiterId, LocalDate date) {
    return databaseClient
        .sql(
            "SELECT * FROM daily_recruiter_summaries "
                + "WHERE organization_id = :organizationId AND recruiter_id = :recruiterId "
                + "AND summary_date = :summaryDate")
        .bind("organizationId", organizationId)
        .bind("recruiterId", recruiterId)
        .bind("summaryDate", date)
        .map((row, metadata) -> map(row))
        .one();
  }

  /** Lists the summaries of a recruiter within a date range, oldest first. */
  public Flux<DailyRecruiterSummary> list(
      UUID organizationId, UUID recruiterId, LocalDate from, LocalDate to) {
    return databaseClient
        .sql(
            "SELECT * FROM daily_recruiter_summaries "
                + "WHERE organization_id = :organizationId AND recruiter_id = :recruiterId "
                + "AND summary_date BETWEEN :from AND :to ORDER BY summary_date")
        .bind("organizationId", organizationId)
        .bind("recruiterId", recruiterId)
        .bind("from", from)
        .bind("to", to)
        .map((row, metadata) -> map(row))
        .all();
  }

  private DailyRecruiterSummary map(Row row) {
    return new DailyRecruiterSummary(
        row.get("summary_date", LocalDate.class),
        row.get("organization_id", UUID.class),
        row.get("recruiter_id", UUID.class),
        toLong(row.get("interviews_held", Long.class)),
        toLong(row.get("interviews_completed", Long.class)),
        toLong(row.get("candidates_contacted", Long.class)),
        row.get("avg_feedback_rating", BigDecimal.class),
        toLong(row.get("violations", Long.class)),
        row.get("created_at", Instant.class),
        row.get("updated_at", Instant.class));
  }

  private static long toLong(Long value) {
    return value == null ? 0L : value;
  }
}
