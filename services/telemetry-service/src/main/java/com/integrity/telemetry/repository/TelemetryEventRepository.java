package com.integrity.telemetry.repository;

import com.integrity.common.R2dbcBindings;
import com.integrity.telemetry.domain.TelemetryEvent;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Database client backed repository for the {@code telemetry_events} partitioned table.
 *
 * <p>The table is RANGE partitioned by month, so all inserts and reads go through the partitioned
 * parent. Inserts are idempotent per {@code (id, occurred_at)} so re-delivered events are ignored.
 */
public final class TelemetryEventRepository {

  private static final String COLUMNS =
      "id, organization_id, session_id, interview_id, event_type, seq, occurred_at, "
          + "client_occurred_at, payload::text AS payload";

  private static final String BIND_ORGANIZATION_ID = "organizationId";
  private static final String BIND_SESSION_ID = "sessionId";

  private final DatabaseClient databaseClient;

  /** Creates a repository bound to the given database client. */
  public TelemetryEventRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  /** Inserts one event, ignoring duplicates on the natural key. */
  public Mono<Void> insertEvent(
      UUID id,
      UUID organizationId,
      UUID sessionId,
      UUID interviewId,
      String eventType,
      long seq,
      Instant occurredAt,
      Instant clientOccurredAt,
      String payload) {
    DatabaseClient.GenericExecuteSpec spec =
        databaseClient
            .sql(
                "INSERT INTO telemetry_events "
                    + "(id, organization_id, session_id, interview_id, event_type, seq, "
                    + " occurred_at, client_occurred_at, payload) "
                    + "VALUES (:id, :organizationId, :sessionId, :interviewId, :eventType, :seq, "
                    + " :occurredAt, :clientOccurredAt, :payload::jsonb) "
                    + "ON CONFLICT (id, occurred_at) DO NOTHING")
            .bind("id", id)
            .bind(BIND_ORGANIZATION_ID, organizationId)
            .bind(BIND_SESSION_ID, sessionId)
            .bind("eventType", eventType)
            .bind("seq", seq)
            .bind("occurredAt", occurredAt)
            .bind("payload", payload);
    spec = R2dbcBindings.bindOrNull(spec, "interviewId", interviewId, UUID.class);
    spec = R2dbcBindings.bindOrNull(spec, "clientOccurredAt", clientOccurredAt, Instant.class);
    return spec.then();
  }

  /** Lists the events of a session, in occurrence order. */
  public Flux<TelemetryEvent> listBySession(UUID organizationId, UUID sessionId) {
    return databaseClient
        .sql(
            "SELECT "
                + COLUMNS
                + " FROM telemetry_events "
                + "WHERE organization_id = :organizationId AND session_id = :sessionId "
                + "ORDER BY occurred_at, seq")
        .bind(BIND_ORGANIZATION_ID, organizationId)
        .bind(BIND_SESSION_ID, sessionId)
        .map((row, metadata) -> map(row))
        .all();
  }

  /** Lists the events of a session filtered by event type, in occurrence order. */
  public Flux<TelemetryEvent> listBySessionAndType(
      UUID organizationId, UUID sessionId, String eventType) {
    return databaseClient
        .sql(
            "SELECT "
                + COLUMNS
                + " FROM telemetry_events "
                + "WHERE organization_id = :organizationId AND session_id = :sessionId "
                + "AND event_type = :eventType ORDER BY occurred_at, seq")
        .bind(BIND_ORGANIZATION_ID, organizationId)
        .bind(BIND_SESSION_ID, sessionId)
        .bind("eventType", eventType)
        .map((row, metadata) -> map(row))
        .all();
  }

  /** Counts the events of a session. */
  public Mono<Long> countBySession(UUID organizationId, UUID sessionId) {
    return databaseClient
        .sql(
            "SELECT count(*) FROM telemetry_events "
                + "WHERE organization_id = :organizationId AND session_id = :sessionId")
        .bind(BIND_ORGANIZATION_ID, organizationId)
        .bind(BIND_SESSION_ID, sessionId)
        .map((row, metadata) -> row.get(0, Long.class))
        .one();
  }

  private TelemetryEvent map(Row row) {
    Long seq = row.get("seq", Long.class);
    return new TelemetryEvent(
        row.get("id", UUID.class),
        row.get("organization_id", UUID.class),
        row.get("session_id", UUID.class),
        row.get("interview_id", UUID.class),
        row.get("event_type", String.class),
        seq == null ? 0L : seq,
        row.get("occurred_at", Instant.class),
        row.get("client_occurred_at", Instant.class),
        row.get("payload", String.class));
  }
}
