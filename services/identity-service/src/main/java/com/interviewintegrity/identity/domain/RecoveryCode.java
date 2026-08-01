package com.interviewintegrity.identity.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** A single-use MFA recovery code, stored as a SHA-256 hash. */
@Table("recovery_codes")
public class RecoveryCode {

  @Id private UUID id;

  @Column("user_id")
  private UUID userId;

  @Column("organization_id")
  private UUID organizationId;

  @Column("code_hash")
  private String codeHash;

  @Column("consumed_at")
  private Instant consumedAt;

  @Column("created_at")
  private Instant createdAt;

  /** Creates a new unused recovery code. */
  public RecoveryCode(UUID userId, UUID organizationId, String codeHash) {
    this.userId = userId;
    this.organizationId = organizationId;
    this.codeHash = codeHash;
    this.createdAt = Instant.now();
  }

  protected RecoveryCode() {}

  /** Marks the code as used. */
  public void consume() {
    this.consumedAt = Instant.now();
  }

  /** Returns true when the code has not been used yet. */
  public boolean isUsable() {
    return consumedAt == null;
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getCodeHash() {
    return codeHash;
  }

  public Instant getConsumedAt() {
    return consumedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setId(UUID id) {
    this.id = id;
  }
}
