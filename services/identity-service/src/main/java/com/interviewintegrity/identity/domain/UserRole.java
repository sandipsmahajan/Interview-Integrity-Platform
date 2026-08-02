package com.interviewintegrity.identity.domain;

import java.time.Instant;
import java.util.UUID;

/** Bridge between a user and its roles. */
public class UserRole {

  private UUID userId;

  private UUID roleId;

  private UUID assignedBy;

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
