package com.interviewintegrity.policy.repository;

import com.interviewintegrity.policy.domain.PolicyRule;
import com.interviewintegrity.policy.domain.ViolationSeverity;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Database client backed repository for the {@code policy_rules} table. */
public final class PolicyRuleRepository {

  private static final String COLUMNS =
      "id, organization_id, policy_id, rule_code, description, condition::text AS condition, "
          + "severity::text AS severity, weight, order_index, enabled, created_by, created_at, "
          + "updated_by, updated_at, deleted_by, deleted_at, version";

  private static final String BIND_ORGANIZATION_ID = "organizationId";

  private final DatabaseClient databaseClient;

  /** Creates a repository bound to the given database client. */
  public PolicyRuleRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  /** Inserts a new rule and returns the persisted row. */
  public Mono<PolicyRule> insert(PolicyRule rule) {
    DatabaseClient.GenericExecuteSpec spec =
        databaseClient
            .sql(
                "INSERT INTO policy_rules "
                    + "(organization_id, policy_id, rule_code, description, condition, severity, "
                    + " weight, order_index, enabled, created_by) "
                    + "VALUES (:organizationId, :policyId, :ruleCode, :description, "
                    + " :condition::jsonb, :severity, :weight, :orderIndex, :enabled, :createdBy) "
                    + "RETURNING "
                    + COLUMNS)
            .bind(BIND_ORGANIZATION_ID, rule.getOrganizationId())
            .bind("policyId", rule.getPolicyId())
            .bind("ruleCode", rule.getRuleCode())
            .bind("condition", rule.getCondition())
            .bind("severity", rule.getSeverity().name())
            .bind("weight", rule.getWeight())
            .bind("orderIndex", rule.getOrderIndex())
            .bind("enabled", rule.isEnabled());
    spec = bindOrNull(spec, "description", rule.getDescription(), String.class);
    spec = bindOrNull(spec, "createdBy", rule.getCreatedBy(), UUID.class);
    return spec.map((row, metadata) -> map(row)).one();
  }

  /** Finds a live rule by id, scoped to an organization. */
  public Mono<PolicyRule> findById(UUID id, UUID organizationId) {
    return databaseClient
        .sql(
            "SELECT "
                + COLUMNS
                + " FROM policy_rules "
                + "WHERE id = :id AND organization_id = :organizationId AND deleted_at IS NULL")
        .bind("id", id)
        .bind(BIND_ORGANIZATION_ID, organizationId)
        .map((row, metadata) -> map(row))
        .one();
  }

  /** Lists the live rules of a policy, in order. */
  public Flux<PolicyRule> listByPolicy(UUID policyId) {
    return databaseClient
        .sql(
            "SELECT "
                + COLUMNS
                + " FROM policy_rules "
                + "WHERE policy_id = :policyId AND deleted_at IS NULL "
                + "ORDER BY order_index ASC, rule_code ASC")
        .bind("policyId", policyId)
        .map((row, metadata) -> map(row))
        .all();
  }

  /** Updates the editable attributes of a rule owned by the organization. */
  public Mono<PolicyRule> update(
      UUID id,
      UUID organizationId,
      String ruleCode,
      String description,
      String condition,
      ViolationSeverity severity,
      Integer weight,
      Integer orderIndex,
      Boolean enabled,
      UUID updatedBy) {
    DatabaseClient.GenericExecuteSpec spec =
        databaseClient
            .sql(
                "UPDATE policy_rules SET rule_code = :ruleCode, condition = :condition::jsonb, "
                    + "severity = :severity, weight = :weight, order_index = :orderIndex, "
                    + "enabled = :enabled, updated_by = :updatedBy, updated_at = now() "
                    + "WHERE id = :id AND organization_id = :organizationId "
                    + "AND deleted_at IS NULL RETURNING "
                    + COLUMNS)
            .bind("id", id)
            .bind(BIND_ORGANIZATION_ID, organizationId)
            .bind("ruleCode", ruleCode)
            .bind("condition", condition)
            .bind("severity", severity.name())
            .bind("weight", weight)
            .bind("orderIndex", orderIndex)
            .bind("enabled", enabled);
    spec = bindOrNull(spec, "description", description, String.class);
    spec = bindOrNull(spec, "updatedBy", updatedBy, UUID.class);
    return spec.map((row, metadata) -> map(row)).one();
  }

  /** Soft deletes a rule owned by the organization. */
  public Mono<Void> softDelete(UUID id, UUID organizationId, UUID deletedBy) {
    DatabaseClient.GenericExecuteSpec spec =
        databaseClient
            .sql(
                "UPDATE policy_rules SET deleted_by = :deletedBy, deleted_at = now(), "
                    + "enabled = FALSE WHERE id = :id AND organization_id = :organizationId "
                    + "AND deleted_at IS NULL")
            .bind("id", id)
            .bind(BIND_ORGANIZATION_ID, organizationId);
    return bindOrNull(spec, "deletedBy", deletedBy, UUID.class).then();
  }

  private PolicyRule map(Row row) {
    Integer weight = row.get("weight", Integer.class);
    Integer orderIndex = row.get("order_index", Integer.class);
    Boolean enabled = row.get("enabled", Boolean.class);
    Long version = row.get("version", Long.class);
    return new PolicyRule(
        row.get("id", UUID.class),
        row.get("organization_id", UUID.class),
        row.get("policy_id", UUID.class),
        row.get("rule_code", String.class),
        row.get("description", String.class),
        row.get("condition", String.class),
        ViolationSeverity.valueOf(row.get("severity", String.class)),
        weight == null ? 0 : weight,
        orderIndex == null ? 0 : orderIndex,
        enabled == null || enabled,
        row.get("created_by", UUID.class),
        row.get("created_at", Instant.class),
        row.get("updated_by", UUID.class),
        row.get("updated_at", Instant.class),
        row.get("deleted_by", UUID.class),
        row.get("deleted_at", Instant.class),
        version == null ? 1L : version);
  }

  private static DatabaseClient.GenericExecuteSpec bindOrNull(
      DatabaseClient.GenericExecuteSpec spec, String name, Object value, Class<?> type) {
    if (value == null) {
      return spec.bindNull(name, type);
    }
    return spec.bind(name, value);
  }
}
