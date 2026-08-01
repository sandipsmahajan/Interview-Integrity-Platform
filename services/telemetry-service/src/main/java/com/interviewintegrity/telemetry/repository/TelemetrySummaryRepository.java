package com.interviewintegrity.telemetry.repository;

import com.interviewintegrity.telemetry.domain.TelemetryEventSummary;
import io.r2dbc.spi.Row;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;

/**
 * Database client backed repository for the {@code telemetry_event_summaries} partitioned table.
 *
 * <p>The rollups are produced by the {@code telemetry_rollup_hour} function; this repository only
 * reads them for dashboards.
 */
public final class TelemetrySummaryRepository {

  private static final String COLUMNS =
      "bucket_start, bucket_end, organization_id, session_id, event_type, event_count, "
          + "min_seq, max_seq, last_payload::text AS last_payload";

  private final DatabaseClient databaseClient;

  /** Creates a repository bound to the given database client. */
  public TelemetrySummaryRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  /** Lists the hourly rollups of a session, oldest bucket first. */
  public Flux<TelemetryEventSummary> listBySession(UUID organizationId, UUID sessionId) {
    return databaseClient
        .sql(
            "SELECT "
                + COLUMNS
                + " FROM telemetry_event_summaries "
                + "WHERE organization_id = :organizationId AND session_id = :sessionId "
                + "ORDER BY bucket_start, event_type")
        .bind("organizationId", organizationId)
        .bind("sessionId", sessionId)
        .map((row, metadata) -> map(row))
        .all();
  }

  private TelemetryEventSummary map(Row row) {
    Long eventCount = row.get("event_count", Long.class);
    return new TelemetryEventSummary(
        row.get("bucket_start", java.time.Instant.class),
        row.get("bucket_end", java.time.Instant.class),
        row.get("organization_id", UUID.class),
        row.get("session_id", UUID.class),
        row.get("event_type", String.class),
        eventCount == null ? 0L : eventCount,
        row.get("min_seq", Long.class),
        row.get("max_seq", Long.class),
        row.get("last_payload", String.class));
  }
}
