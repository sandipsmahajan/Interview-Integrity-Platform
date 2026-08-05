package com.integrity.organization.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Row mapped membership of a user in a team.
 *
 * <p>The bridge table uses a composite primary key and references the user through a soft
 * cross-database reference into identity_db, so it is read through {@code DatabaseClient} SQL.
 */
@Table("team_members")
public class TeamMember {

  @Column("team_id")
  private UUID teamId;

  @Column("user_id")
  private UUID userId;

  @Column("added_by")
  private UUID addedBy;

  @Column("added_at")
  private Instant addedAt;

  protected TeamMember() {}

  /** Returns the id of the team. */
  public UUID getTeamId() {
    return teamId;
  }

  /** Returns the id of the member user. */
  public UUID getUserId() {
    return userId;
  }

  /** Returns the id of the user that added the member. */
  public UUID getAddedBy() {
    return addedBy;
  }

  /** Returns the instant the member was added. */
  public Instant getAddedAt() {
    return addedAt;
  }
}
