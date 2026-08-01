package com.interviewintegrity.identity.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Bridge between a user and its roles. */
@Table("user_roles")
public class UserRole {

  @Id
  @Column("user_id")
  private UUID userId;

  @Id
  @Column("role_id")
  private UUID roleId;

  @Column("assigned_by")
  private UUID assignedBy;

  @Column("assigned_at")
  private Instant assignedAt;

  /** Creates a new assignment. */
  public UserRole(UUID userId, UUID roleId, UUID assignedBy) {
    this.userId = userId;
    this.roleId = roleId;
    this.assignedBy = assignedBy;
    this.assignedAt = Instant.now();
  }

  protected UserRole() {}

  public UUID getUserId() {
    return userId;
  }

  public UUID getRoleId() {
    return roleId;
  }

  public UUID getAssignedBy() {
    return assignedBy;
  }

  public Instant getAssignedAt() {
    return assignedAt;
  }
}
