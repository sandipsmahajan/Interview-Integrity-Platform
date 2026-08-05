package com.integrity.candidate.repository;

import com.integrity.candidate.domain.Tag;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Database client backed repository for the {@code candidate_tags} bridge table.
 *
 * <p>The bridge has a composite primary key, which Spring Data R2DBC entities cannot map directly,
 * so explicit SQL is used for all operations.
 */
public final class CandidateTagRepository {

  private static final String CANDIDATE_ID = "candidateId";
  private static final String TAG_ID = "tagId";
  private static final String TAGGED_BY = "taggedBy";

  private final DatabaseClient databaseClient;

  /** Creates a repository bound to the given database client. */
  public CandidateTagRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  /** Tags a candidate, ignoring a duplicate tagging. */
  public Mono<Void> add(UUID candidateId, UUID tagId, UUID taggedBy) {
    return databaseClient
        .sql(
            "INSERT INTO candidate_tags (candidate_id, tag_id, tagged_by, tagged_at) "
                + "VALUES (:candidateId, :tagId, :taggedBy, now()) ON CONFLICT DO NOTHING")
        .bind(CANDIDATE_ID, candidateId)
        .bind(TAG_ID, tagId)
        .bind(TAGGED_BY, taggedBy)
        .then();
  }

  /** Removes a tag from a candidate. */
  public Mono<Void> remove(UUID candidateId, UUID tagId) {
    return databaseClient
        .sql("DELETE FROM candidate_tags WHERE candidate_id = :candidateId AND tag_id = :tagId")
        .bind(CANDIDATE_ID, candidateId)
        .bind(TAG_ID, tagId)
        .then();
  }

  /** Lists the tags applied to a candidate, ordered by code. */
  public Flux<Tag> listTagsByCandidate(UUID candidateId) {
    return databaseClient
        .sql(
            "SELECT t.id, t.organization_id, t.code, t.name, t.created_at, t.version "
                + "FROM tags t JOIN candidate_tags ct ON ct.tag_id = t.id "
                + "WHERE ct.candidate_id = :candidateId ORDER BY t.code")
        .bind(CANDIDATE_ID, candidateId)
        .mapProperties(Tag.class)
        .all();
  }
}
