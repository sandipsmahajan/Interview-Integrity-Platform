package com.interviewintegrity.policy.repository;

import com.interviewintegrity.policy.domain.PolicyStatus;
import com.interviewintegrity.policy.domain.PolicyVersion;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Database client backed repository for the {@code policy_versions} table. */
public final class PolicyVersionRepository {

  private static final String COLUMNS =
      "id, organization_id, policy_id, version, definition::text AS definition, "
          + "status::text AS status, published_by, published_at, created_at";

  private final DatabaseClient databaseClient;

  /** Creates a repository bound to the given database client. */
  public PolicyVersionRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  /** Inserts a version snapshot and returns the persisted row. */
  public Mono<PolicyVersion> insert(PolicyVersion version) {
    DatabaseClient.GenericExecuteSpec spec =
        databaseClient
            .sql(
                "INSERT INTO policy_versions "
                    + "(organization_id, policy_id, version, definition, status, published_by, "
                    + " published_at) "
                    + "VALUES (:organizationId, :policyId, :version, :definition::jsonb, "
                    + " :status, :publishedBy, :publishedAt) RETURNING "
                    + COLUMNS)
            .bind("organizationId", version.getOrganizationId())
            .bind("policyId", version.getPolicyId())
            .bind("version", version.getVersion())
            .bind("definition", version.getDefinition())
            .bind("status", version.getStatus().name())
            .bind("publishedAt", version.getPublishedAt());
    return bindOrNull(spec, "publishedBy", version.getPublishedBy(), UUID.class)
        .map((row, metadata) -> map(row))
        .one();
  }

  /** Lists the versions of a policy, newest first. */
  public Flux<PolicyVersion> listByPolicy(UUID policyId) {
    return databaseClient
        .sql(
            "SELECT "
                + COLUMNS
                + " FROM policy_versions "
                + "WHERE policy_id = :policyId ORDER BY version DESC")
        .bind("policyId", policyId)
        .map((row, metadata) -> map(row))
        .all();
  }

  /** Returns the highest version number recorded for a policy. */
  public Mono<Integer> latestVersion(UUID policyId) {
    return databaseClient
        .sql(
            "SELECT COALESCE(max(version), 0) FROM policy_versions "
                + "WHERE policy_id = :policyId")
        .bind("policyId", policyId)
        .map((row, metadata) -> row.get(0, Integer.class))
        .one();
  }

  private PolicyVersion map(Row row) {
    Integer version = row.get("version", Integer.class);
    return new PolicyVersion(
        row.get("id", UUID.class),
        row.get("organization_id", UUID.class),
        row.get("policy_id", UUID.class),
        version == null ? 0 : version,
        row.get("definition", String.class),
        PolicyStatus.valueOf(row.get("status", String.class)),
        row.get("published_by", UUID.class),
        row.get("published_at", Instant.class),
        row.get("created_at", Instant.class));
  }

  private static DatabaseClient.GenericExecuteSpec bindOrNull(
      DatabaseClient.GenericExecuteSpec spec, String name, Object value, Class<?> type) {
    if (value == null) {
      return spec.bindNull(name, type);
    }
    return spec.bind(name, value);
  }
}
