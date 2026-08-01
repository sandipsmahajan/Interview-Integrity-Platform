package com.interviewintegrity.identity.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Historical password hash enabling password reuse prevention. */
@Table("password_history")
public class PasswordHistory {

  @Id private UUID id;

  @Column("user_id")
  private UUID userId;

  @Column("password_hash")
  private String passwordHash;

  @Column("changed_by")
  private UUID changedBy;

  @Column("changed_at")
  private Instant changedAt;

  /** Creates a history entry for a password change. */
  public PasswordHistory(UUID userId, String passwordHash, UUID changedBy) {
    this.userId = userId;
    this.passwordHash = passwordHash;
    this.changedBy = changedBy;
    this.changedAt = Instant.now();
  }

  protected PasswordHistory() {}

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public UUID getChangedBy() {
    return changedBy;
  }

  public Instant getChangedAt() {
    return changedAt;
  }

  public void setId(UUID id) {
    this.id = id;
  }
}
