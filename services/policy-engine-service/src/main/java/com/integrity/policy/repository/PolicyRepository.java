package com.integrity.policy.repository;

import com.integrity.common.R2dbcBindings;
import com.integrity.policy.domain.Policy;
import com.integrity.policy.domain.PolicyStatus;
import com.integrity.policy.domain.ViolationSeverity;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Database client backed repository for the {@code policies} table. */
public final class PolicyRepository {

  private static final String COLUMNS =
      "id, organization_id, code, name, description, status::text AS status, "
          + "default_severity::text AS default_severity, priority, enabled, created_by, "
          + "created_at, updated_by, updated_at, deleted_by, deleted_at, version";

  private static final String BIND_ORGANIZATION_ID = "organizationId";
  private static final String BIND_STATUS = "status";

  private final DatabaseClient databaseClient;

  /** Creates a repository bound to the given database client. */
  public PolicyRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  /** Inserts a new policy and returns the persisted row. */
  public Mono<Policy> insert(Policy policy) {
    DatabaseClient.GenericExecuteSpec spec =
        databaseClient
            .sql(
                "INSERT INTO policies "
                    + "(organization_id, code, name, description, status, default_severity, "
                    + " priority, enabled, created_by) "
                    + "VALUES (:organizationId, :code, :name, :description, :status, "
                    + " :defaultSeverity, :priority, :enabled, :createdBy) RETURNING "
                    + COLUMNS)
            .bind(BIND_ORGANIZATION_ID, policy.getOrganizationId())
            .bind("code", policy.getCode())
            .bind("name", policy.getName())
            .bind(BIND_STATUS, policy.getStatus().name())
            .bind("defaultSeverity", policy.getDefaultSeverity().name())
            .bind("priority", policy.getPriority())
            .bind("enabled", policy.isEnabled());
    spec = R2dbcBindings.bindOrNull(spec, "description", policy.getDescription(), String.class);
    spec = R2dbcBindings.bindOrNull(spec, "createdBy", policy.getCreatedBy(), UUID.class);
    return spec.map((row, metadata) -> map(row)).one();
  }

  /** Finds a live policy by id. */
  public Mono<Policy> findById(UUID id) {
    return databaseClient
        .sql("SELECT " + COLUMNS + " FROM policies WHERE id = :id AND deleted_at IS NULL")
        .bind("id", id)
        .map((row, metadata) -> map(row))
        .one();
  }

  /** Lists the live policies of an organization, highest priority first. */
  public Flux<Policy> listByOrganization(UUID organizationId) {
    return databaseClient
        .sql(
            "SELECT "
                + COLUMNS
                + " FROM policies "
                + "WHERE organization_id = :organizationId AND deleted_at IS NULL "
                + "ORDER BY priority ASC, code ASC")
        .bind(BIND_ORGANIZATION_ID, organizationId)
        .map((row, metadata) -> map(row))
        .all();
  }

  /** Updates the editable attributes of a policy owned by the organization. */
  public Mono<Policy> update(
      UUID id,
      UUID organizationId,
      String name,
      String description,
      PolicyStatus status,
      ViolationSeverity defaultSeverity,
      Integer priority,
      Boolean enabled,
      UUID updatedBy) {
    DatabaseClient.GenericExecuteSpec spec =
        databaseClient
            .sql(
                "UPDATE policies SET name = :name, status = :status, "
                    + "default_severity = :defaultSeverity, priority = :priority, "
                    + "enabled = :enabled, updated_by = :updatedBy, updated_at = now() "
                    + "WHERE id = :id AND organization_id = :organizationId "
                    + "AND deleted_at IS NULL RETURNING "
                    + COLUMNS)
            .bind("id", id)
            .bind(BIND_ORGANIZATION_ID, organizationId)
            .bind("name", name)
            .bind(BIND_STATUS, status.name())
            .bind("defaultSeverity", defaultSeverity.name())
            .bind("priority", priority)
            .bind("enabled", enabled);
    spec = R2dbcBindings.bindOrNull(spec, "description", description, String.class);
    spec = R2dbcBindings.bindOrNull(spec, "updatedBy", updatedBy, UUID.class);
    return spec.map((row, metadata) -> map(row)).one();
  }

  /** Applies a lifecycle transition to a policy owned by the organization. */
  public Mono<Policy> changeStatus(
      UUID id, UUID organizationId, PolicyStatus status, UUID updatedBy) {
    DatabaseClient.GenericExecuteSpec spec =
        databaseClient
            .sql(
                "UPDATE policies SET status = :status, updated_by = :updatedBy, "
                    + "updated_at = now() WHERE id = :id AND organization_id = :organizationId "
                    + "AND deleted_at IS NULL RETURNING "
                    + COLUMNS)
            .bind("id", id)
            .bind(BIND_ORGANIZATION_ID, organizationId)
            .bind(BIND_STATUS, status.name());
    return R2dbcBindings.bindOrNull(spec, "updatedBy", updatedBy, UUID.class)
        .map((row, metadata) -> map(row))
        .one();
  }

  /** Soft deletes a policy owned by the organization. */
  public Mono<Void> softDelete(UUID id, UUID organizationId, UUID deletedBy) {
    DatabaseClient.GenericExecuteSpec spec =
        databaseClient
            .sql(
                "UPDATE policies SET deleted_by = :deletedBy, deleted_at = now(), "
                    + "enabled = FALSE WHERE id = :id AND organization_id = :organizationId "
                    + "AND deleted_at IS NULL")
            .bind("id", id)
            .bind(BIND_ORGANIZATION_ID, organizationId);
    return R2dbcBindings.bindOrNull(spec, "deletedBy", deletedBy, UUID.class).then();
  }

  private Policy map(Row row) {
    Integer priority = row.get("priority", Integer.class);
    Boolean enabled = row.get("enabled", Boolean.class);
    Long version = row.get("version", Long.class);
    return new Policy(
        row.get("id", UUID.class),
        row.get("organization_id", UUID.class),
        row.get("code", String.class),
        row.get("name", String.class),
        row.get("description", String.class),
        PolicyStatus.valueOf(row.get("status", String.class)),
        ViolationSeverity.valueOf(row.get("default_severity", String.class)),
        priority == null ? 0 : priority,
        enabled == null || enabled,
        row.get("created_by", UUID.class),
        row.get("created_at", Instant.class),
        row.get("updated_by", UUID.class),
        row.get("updated_at", Instant.class),
        row.get("deleted_by", UUID.class),
        row.get("deleted_at", Instant.class),
        version == null ? 1L : version);
  }
}
