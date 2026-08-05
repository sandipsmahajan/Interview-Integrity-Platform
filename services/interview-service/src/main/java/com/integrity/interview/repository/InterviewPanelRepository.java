package com.integrity.interview.repository;

import com.integrity.interview.domain.InterviewPanel;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Database client backed repository for the {@code interview_panels} bridge table.
 *
 * <p>The bridge has a composite primary key, which Spring Data R2DBC entities cannot map directly,
 * so explicit SQL is used for all operations.
 */
public final class InterviewPanelRepository {

  private static final String INTERVIEW_ID = "interviewId";
  private static final String INTERVIEWER_ID = "interviewerId";
  private static final String ROLE = "role";
  private static final String ADDED_BY = "addedBy";

  private final DatabaseClient databaseClient;

  /** Creates a repository bound to the given database client. */
  public InterviewPanelRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  /** Adds an interviewer to a panel, ignoring a duplicate membership. */
  public Mono<Void> add(UUID interviewId, UUID interviewerId, String role, UUID addedBy) {
    return databaseClient
        .sql(
            "INSERT INTO interview_panels (interview_id, interviewer_id, role, added_by, added_at) "
                + "VALUES (:interviewId, :interviewerId, :role, :addedBy, now()) "
                + "ON CONFLICT DO NOTHING")
        .bind(INTERVIEW_ID, interviewId)
        .bind(INTERVIEWER_ID, interviewerId)
        .bind(ROLE, role)
        .bind(ADDED_BY, addedBy)
        .then();
  }

  /** Removes an interviewer from a panel. */
  public Mono<Void> remove(UUID interviewId, UUID interviewerId) {
    return databaseClient
        .sql(
            "DELETE FROM interview_panels WHERE interview_id = :interviewId "
                + "AND interviewer_id = :interviewerId")
        .bind(INTERVIEW_ID, interviewId)
        .bind(INTERVIEWER_ID, interviewerId)
        .then();
  }

  /** Returns true when the interviewer is already on the panel. */
  public Mono<Boolean> exists(UUID interviewId, UUID interviewerId) {
    return databaseClient
        .sql(
            "SELECT count(*) FROM interview_panels WHERE interview_id = :interviewId "
                + "AND interviewer_id = :interviewerId")
        .bind(INTERVIEW_ID, interviewId)
        .bind(INTERVIEWER_ID, interviewerId)
        .map((row, metadata) -> row.get(0, Long.class))
        .one()
        .map(count -> count > 0);
  }

  /** Lists the interviewers on a panel, oldest first. */
  public Flux<InterviewPanel> listByInterview(UUID interviewId) {
    return databaseClient
        .sql("SELECT * FROM interview_panels WHERE interview_id = :interviewId ORDER BY added_at")
        .bind(INTERVIEW_ID, interviewId)
        .mapProperties(InterviewPanel.class)
        .all();
  }
}
