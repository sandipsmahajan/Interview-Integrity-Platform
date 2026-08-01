package com.interviewintegrity.telemetry.repository;

import com.interviewintegrity.telemetry.domain.TelemetryEventType;
import io.r2dbc.spi.Row;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Database client backed repository for the global {@code telemetry_event_types} catalog. */
public final class TelemetryEventTypeRepository {

  private static final String COLUMNS =
      "id, code, name, description, retention_days, created_at, updated_at, version";

  private final DatabaseClient databaseClient;

  /** Creates a repository bound to the given database client. */
  public TelemetryEventTypeRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  /** Lists the event type catalog, ordered by code. */
  public Flux<TelemetryEventType> list() {
    return databaseClient
        .sql("SELECT " + COLUMNS + " FROM telemetry_event_types ORDER BY code")
        .map((row, metadata) -> map(row))
        .all();
  }

  /** Finds a single event type by code. */
  public Mono<TelemetryEventType> findByCode(String code) {
    return databaseClient
        .sql("SELECT " + COLUMNS + " FROM telemetry_event_types WHERE code = :code")
        .bind("code", code)
        .map((row, metadata) -> map(row))
        .one();
  }

  private TelemetryEventType map(Row row) {
    Integer retentionDays = row.get("retention_days", Integer.class);
    Long version = row.get("version", Long.class);
    return new TelemetryEventType(
        row.get("id", UUID.class),
        row.get("code", String.class),
        row.get("name", String.class),
        row.get("description", String.class),
        retentionDays == null ? 0 : retentionDays,
        row.get("created_at", java.time.Instant.class),
        row.get("updated_at", java.time.Instant.class),
        version == null ? 1L : version);
  }
}
