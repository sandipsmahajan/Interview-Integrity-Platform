package com.integrity.policy.repository;

import com.integrity.common.R2dbcBindings;
import com.integrity.policy.domain.ViolationEscalation;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Database client backed repository for the {@code violation_escalations} table. */
public final class ViolationEscalationRepository {

  private static final String COLUMNS =
      "id, organization_id, violation_id, escalated_to, reason, escalated_by, escalated_at, "
          + "resolved_at, resolution";

  private final DatabaseClient databaseClient;

  /** Creates a repository bound to the given database client. */
  public ViolationEscalationRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  /** Inserts an escalation and returns the persisted row. */
  public Mono<ViolationEscalation> insert(ViolationEscalation escalation) {
    DatabaseClient.GenericExecuteSpec spec =
        databaseClient
            .sql(
                "INSERT INTO violation_escalations "
                    + "(organization_id, violation_id, escalated_to, reason, escalated_by) "
                    + "VALUES (:organizationId, :violationId, :escalatedTo, :reason, "
                    + " :escalatedBy) RETURNING "
                    + COLUMNS)
            .bind("organizationId", escalation.getOrganizationId())
            .bind("violationId", escalation.getViolationId())
            .bind("escalatedTo", escalation.getEscalatedTo());
    spec = R2dbcBindings.bindOrNull(spec, "reason", escalation.getReason(), String.class);
    spec = R2dbcBindings.bindOrNull(spec, "escalatedBy", escalation.getEscalatedBy(), UUID.class);
    return spec.map((row, metadata) -> map(row)).one();
  }

  /** Lists the open escalations raised for a violation. */
  public Flux<ViolationEscalation> listOpenByViolation(UUID violationId) {
    return databaseClient
        .sql(
            "SELECT "
                + COLUMNS
                + " FROM violation_escalations "
                + "WHERE violation_id = :violationId AND resolved_at IS NULL "
                + "ORDER BY escalated_at DESC")
        .bind("violationId", violationId)
        .map((row, metadata) -> map(row))
        .all();
  }

  private ViolationEscalation map(Row row) {
    return new ViolationEscalation(
        row.get("id", UUID.class),
        row.get("organization_id", UUID.class),
        row.get("violation_id", UUID.class),
        row.get("escalated_to", UUID.class),
        row.get("reason", String.class),
        row.get("escalated_by", UUID.class),
        row.get("escalated_at", Instant.class),
        row.get("resolved_at", Instant.class),
        row.get("resolution", String.class));
  }
}
