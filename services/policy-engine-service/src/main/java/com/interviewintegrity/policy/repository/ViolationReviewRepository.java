package com.interviewintegrity.policy.repository;

import com.interviewintegrity.policy.domain.ReviewAction;
import com.interviewintegrity.policy.domain.ViolationReview;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Database client backed repository for the {@code violation_reviews} table. */
public final class ViolationReviewRepository {

  private static final String COLUMNS =
      "id, organization_id, violation_id, reviewer_id, action::text AS action, comment, "
          + "reviewed_at";

  private final DatabaseClient databaseClient;

  /** Creates a repository bound to the given database client. */
  public ViolationReviewRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  /** Inserts a review and returns the persisted row. */
  public Mono<ViolationReview> insert(ViolationReview review) {
    DatabaseClient.GenericExecuteSpec spec =
        databaseClient
            .sql(
                "INSERT INTO violation_reviews "
                    + "(organization_id, violation_id, reviewer_id, action, comment) "
                    + "VALUES (:organizationId, :violationId, :reviewerId, :action, :comment) "
                    + "RETURNING "
                    + COLUMNS)
            .bind("organizationId", review.getOrganizationId())
            .bind("violationId", review.getViolationId())
            .bind("reviewerId", review.getReviewerId())
            .bind("action", review.getAction().name());
    return bindOrNull(spec, "comment", review.getComment(), String.class)
        .map((row, metadata) -> map(row))
        .one();
  }

  /** Lists the reviews recorded for a violation, newest first. */
  public Flux<ViolationReview> listByViolation(UUID violationId) {
    return databaseClient
        .sql(
            "SELECT "
                + COLUMNS
                + " FROM violation_reviews "
                + "WHERE violation_id = :violationId ORDER BY reviewed_at DESC")
        .bind("violationId", violationId)
        .map((row, metadata) -> map(row))
        .all();
  }

  private ViolationReview map(Row row) {
    return new ViolationReview(
        row.get("id", UUID.class),
        row.get("organization_id", UUID.class),
        row.get("violation_id", UUID.class),
        row.get("reviewer_id", UUID.class),
        ReviewAction.valueOf(row.get("action", String.class)),
        row.get("comment", String.class),
        row.get("reviewed_at", Instant.class));
  }

  private static DatabaseClient.GenericExecuteSpec bindOrNull(
      DatabaseClient.GenericExecuteSpec spec, String name, Object value, Class<?> type) {
    if (value == null) {
      return spec.bindNull(name, type);
    }
    return spec.bind(name, value);
  }
}
