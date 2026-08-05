package com.integrity.policy.repository;

import com.integrity.common.R2dbcBindings;
import com.integrity.policy.domain.Violation;
import com.integrity.policy.domain.ViolationSeverity;
import com.integrity.policy.domain.ViolationStatus;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Database client backed repository for the {@code violations} table. */
public final class ViolationRepository {

  private static final String COLUMNS =
      "id, organization_id, session_id, interview_id, policy_id, rule_code, "
          + "severity::text AS severity, message, status::text AS status, "
          + "evidence::text AS evidence, occurred_at, detected_by, created_at, updated_at, version";

  private static final String BIND_ORGANIZATION_ID = "organizationId";
  private static final String BIND_STATUS = "status";

  private final DatabaseClient databaseClient;

  /** Creates a repository bound to the given database client. */
  public ViolationRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  /** Inserts a violation and returns the persisted row. */
  public Mono<Violation> insert(Violation violation) {
    DatabaseClient.GenericExecuteSpec spec =
        databaseClient
            .sql(
                "INSERT INTO violations "
                    + "(organization_id, session_id, interview_id, policy_id, rule_code, "
                    + " severity, message, status, evidence, occurred_at, detected_by) "
                    + "VALUES (:organizationId, :sessionId, :interviewId, :policyId, :ruleCode, "
                    + " :severity, :message, :status, :evidence::jsonb, :occurredAt, "
                    + " :detectedBy) RETURNING "
                    + COLUMNS)
            .bind(BIND_ORGANIZATION_ID, violation.getOrganizationId())
            .bind("sessionId", violation.getSessionId())
            .bind("ruleCode", violation.getRuleCode())
            .bind("severity", violation.getSeverity().name())
            .bind("message", violation.getMessage())
            .bind(BIND_STATUS, violation.getStatus().name())
            .bind("occurredAt", violation.getOccurredAt());
    spec = R2dbcBindings.bindOrNull(spec, "interviewId", violation.getInterviewId(), UUID.class);
    spec = R2dbcBindings.bindOrNull(spec, "policyId", violation.getPolicyId(), UUID.class);
    spec = R2dbcBindings.bindOrNull(spec, "evidence", violation.getEvidence(), String.class);
    spec = R2dbcBindings.bindOrNull(spec, "detectedBy", violation.getDetectedBy(), String.class);
    return spec.map((row, metadata) -> map(row)).one();
  }

  /** Finds a violation by id. */
  public Mono<Violation> findById(UUID id) {
    return databaseClient
        .sql("SELECT " + COLUMNS + " FROM violations WHERE id = :id")
        .bind("id", id)
        .map((row, metadata) -> map(row))
        .one();
  }

  /** Checks whether a violation with the same fingerprint already exists. */
  public Mono<Boolean> exists(UUID sessionId, String ruleCode, Instant occurredAt) {
    return databaseClient
        .sql(
            "SELECT EXISTS (SELECT 1 FROM violations "
                + "WHERE session_id = :sessionId AND rule_code = :ruleCode "
                + "AND occurred_at = :occurredAt)")
        .bind("sessionId", sessionId)
        .bind("ruleCode", ruleCode)
        .bind("occurredAt", occurredAt)
        .map((row, metadata) -> row.get(0, Boolean.class))
        .one();
  }

  /** Lists the violations of an organization, newest first. */
  public Flux<Violation> listByOrganization(
      UUID organizationId, ViolationStatus status, ViolationSeverity severity) {
    StringBuilder sql =
        new StringBuilder(
            "SELECT " + COLUMNS + " FROM violations WHERE organization_id = :organizationId");
    if (status != null) {
      sql.append(" AND status = :status");
    }
    if (severity != null) {
      sql.append(" AND severity = :severity");
    }
    sql.append(" ORDER BY occurred_at DESC");
    DatabaseClient.GenericExecuteSpec spec =
        databaseClient.sql(sql.toString()).bind(BIND_ORGANIZATION_ID, organizationId);
    if (status != null) {
      spec = spec.bind(BIND_STATUS, status.name());
    }
    if (severity != null) {
      spec = spec.bind("severity", severity.name());
    }
    return spec.map((row, metadata) -> map(row)).all();
  }

  /** Transitions a violation to a new triage state. */
  public Mono<Violation> updateStatus(UUID id, UUID organizationId, ViolationStatus status) {
    return databaseClient
        .sql(
            "UPDATE violations SET status = :status, updated_at = now() "
                + "WHERE id = :id AND organization_id = :organizationId RETURNING "
                + COLUMNS)
        .bind("id", id)
        .bind(BIND_ORGANIZATION_ID, organizationId)
        .bind(BIND_STATUS, status.name())
        .map((row, metadata) -> map(row))
        .one();
  }

  private Violation map(Row row) {
    Long version = row.get("version", Long.class);
    return new Violation(
        row.get("id", UUID.class),
        row.get("organization_id", UUID.class),
        row.get("session_id", UUID.class),
        row.get("interview_id", UUID.class),
        row.get("policy_id", UUID.class),
        row.get("rule_code", String.class),
        ViolationSeverity.valueOf(row.get("severity", String.class)),
        row.get("message", String.class),
        ViolationStatus.valueOf(row.get("status", String.class)),
        row.get("evidence", String.class),
        row.get("occurred_at", Instant.class),
        row.get("detected_by", String.class),
        row.get("created_at", Instant.class),
        row.get("updated_at", Instant.class),
        version == null ? 1L : version);
  }
}
