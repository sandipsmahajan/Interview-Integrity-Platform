package com.interviewintegrity.organization.repository;

import com.interviewintegrity.organization.domain.TeamMember;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Database client backed repository for the {@code team_members} bridge table.
 *
 * <p>The bridge has a composite primary key, which Spring Data R2DBC entities cannot map directly,
 * so explicit SQL is used for all operations.
 */
public final class TeamMemberRepository {

  private static final String TEAM_ID = "teamId";
  private static final String USER_ID = "userId";
  private static final String ADDED_BY = "addedBy";

  private final DatabaseClient databaseClient;

  /** Creates a repository bound to the given database client. */
  public TeamMemberRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  /** Adds a user to a team, ignoring a duplicate membership. */
  public Mono<Void> add(UUID teamId, UUID userId, UUID addedBy) {
    return databaseClient
        .sql(
            "INSERT INTO team_members (team_id, user_id, added_by, added_at) "
                + "VALUES (:teamId, :userId, :addedBy, now()) ON CONFLICT DO NOTHING")
        .bind(TEAM_ID, teamId)
        .bind(USER_ID, userId)
        .bind(ADDED_BY, addedBy)
        .then();
  }

  /** Removes a user from a team. */
  public Mono<Void> remove(UUID teamId, UUID userId) {
    return databaseClient
        .sql("DELETE FROM team_members WHERE team_id = :teamId AND user_id = :userId")
        .bind(TEAM_ID, teamId)
        .bind(USER_ID, userId)
        .then();
  }

  /** Returns true when the user is already a member of the team. */
  public Mono<Boolean> exists(UUID teamId, UUID userId) {
    return databaseClient
        .sql("SELECT count(*) FROM team_members WHERE team_id = :teamId AND user_id = :userId")
        .bind(TEAM_ID, teamId)
        .bind(USER_ID, userId)
        .map((row, metadata) -> row.get(0, Long.class))
        .one()
        .map(count -> count > 0);
  }

  /** Lists the members of a team, newest first. */
  public Flux<TeamMember> listByTeam(UUID teamId) {
    return databaseClient
        .sql("SELECT * FROM team_members WHERE team_id = :teamId ORDER BY added_at DESC")
        .bind(TEAM_ID, teamId)
        .mapProperties(TeamMember.class)
        .all();
  }

  /** Lists the teams a user belongs to, newest first. */
  public Flux<TeamMember> listByUser(UUID userId) {
    return databaseClient
        .sql("SELECT * FROM team_members WHERE user_id = :userId ORDER BY added_at DESC")
        .bind(USER_ID, userId)
        .mapProperties(TeamMember.class)
        .all();
  }
}
