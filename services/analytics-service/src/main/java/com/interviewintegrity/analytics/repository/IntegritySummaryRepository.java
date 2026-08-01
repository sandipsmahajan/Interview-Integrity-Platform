package com.interviewintegrity.analytics.repository;

import com.interviewintegrity.analytics.domain.DailyIntegritySummary;
import io.r2dbc.spi.Row;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Database client backed repository for the {@code daily_integrity_summaries} table.
 *
 * <p>The table uses a composite primary key (summary_date, organization_id), which Spring Data
 * R2DBC entities cannot map directly, so explicit SQL is used for all operations.
 */
public final class IntegritySummaryRepository {

  private final DatabaseClient databaseClient;

  /** Creates a repository bound to the given database client. */
  public IntegritySummaryRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  /** Upserts a daily integrity summary (idempotent per primary key). */
  public Mono<DailyIntegritySummary> upsert(DailyIntegritySummary summary) {
    DatabaseClient.GenericExecuteSpec spec =
        databaseClient
            .sql(
                "INSERT INTO daily_integrity_summaries "
                    + "(summary_date, organization_id, total_events, violations_total, "
                    + " violations_by_severity, violations_by_rule, sessions_started, "
                    + " sessions_abandoned, avg_heartbeat_cadence_seconds, created_at, updated_at) "
                    + "VALUES (:summaryDate, :organizationId, :totalEvents, :violationsTotal, "
                    + " :violationsBySeverity, :violationsByRule, :sessionsStarted, "
                    + " :sessionsAbandoned, :avgCadence, now(), now()) "
                    + "ON CONFLICT (summary_date, organization_id) DO UPDATE SET "
                    + " total_events = EXCLUDED.total_events, "
                    + " violations_total = EXCLUDED.violations_total, "
                    + " violations_by_severity = EXCLUDED.violations_by_severity, "
                    + " violations_by_rule = EXCLUDED.violations_by_rule, "
                    + " sessions_started = EXCLUDED.sessions_started, "
                    + " sessions_abandoned = EXCLUDED.sessions_abandoned, "
                    + " avg_heartbeat_cadence_seconds = EXCLUDED.avg_heartbeat_cadence_seconds, "
                    + " updated_at = now()")
            .bind("summaryDate", summary.getSummaryDate())
            .bind("organizationId", summary.getOrganizationId())
            .bind("totalEvents", summary.getTotalEvents())
            .bind("violationsTotal", summary.getViolationsTotal())
            .bind("violationsBySeverity", summary.getViolationsBySeverity())
            .bind("violationsByRule", summary.getViolationsByRule())
            .bind("sessionsStarted", summary.getSessionsStarted())
            .bind("sessionsAbandoned", summary.getSessionsAbandoned());
    if (summary.getAvgHeartbeatCadenceSeconds() != null) {
      spec = spec.bind("avgCadence", summary.getAvgHeartbeatCadenceSeconds());
    } else {
      spec = spec.bindNull("avgCadence", BigDecimal.class);
    }
    return spec.then().thenReturn(summary);
  }

  /** Finds the summary for an organization on a specific date. */
  public Mono<DailyIntegritySummary> find(UUID organizationId, LocalDate date) {
    return databaseClient
        .sql(
            "SELECT * FROM daily_integrity_summaries "
                + "WHERE organization_id = :organizationId AND summary_date = :summaryDate")
        .bind("organizationId", organizationId)
        .bind("summaryDate", date)
        .map((row, metadata) -> map(row))
        .one();
  }

  /** Lists the summaries of an organization within a date range, oldest first. */
  public Flux<DailyIntegritySummary> list(UUID organizationId, LocalDate from, LocalDate to) {
    return databaseClient
        .sql(
            "SELECT * FROM daily_integrity_summaries "
                + "WHERE organization_id = :organizationId AND summary_date BETWEEN :from AND :to "
                + "ORDER BY summary_date")
        .bind("organizationId", organizationId)
        .bind("from", from)
        .bind("to", to)
        .map((row, metadata) -> map(row))
        .all();
  }

  private DailyIntegritySummary map(Row row) {
    return new DailyIntegritySummary(
        row.get("summary_date", LocalDate.class),
        row.get("organization_id", UUID.class),
        toLong(row.get("total_events", Long.class)),
        toLong(row.get("violations_total", Long.class)),
        row.get("violations_by_severity", String.class),
        row.get("violations_by_rule", String.class),
        toLong(row.get("sessions_started", Long.class)),
        toLong(row.get("sessions_abandoned", Long.class)),
        row.get("avg_heartbeat_cadence_seconds", BigDecimal.class),
        row.get("created_at", Instant.class),
        row.get("updated_at", Instant.class));
  }

  private static long toLong(Long value) {
    return value == null ? 0L : value;
  }
}
