package com.interviewintegrity.identity.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** A short-lived one-time passcode delivered by email for a specific purpose. */
@Table("otp_codes")
public class OtpCode {

  @Id private UUID id;

  @Column("user_id")
  private UUID userId;

  @Column("organization_id")
  private UUID organizationId;

  private String purpose;

  @Column("code_hash")
  private String codeHash;

  private int attempts;

  @Column("max_attempts")
  private int maxAttempts;

  @Column("expires_at")
  private Instant expiresAt;

  @Column("consumed_at")
  private Instant consumedAt;

  @Column("requested_at")
  private Instant requestedAt;

  @Column("created_at")
  private Instant createdAt;

  /** Creates a new outstanding OTP code for the user and purpose. */
  public OtpCode(
      UUID userId,
      UUID organizationId,
      String purpose,
      String codeHash,
      int maxAttempts,
      Instant expiresAt) {
    this.userId = userId;
    this.organizationId = organizationId;
    this.purpose = purpose;
    this.codeHash = codeHash;
    this.attempts = 0;
    this.maxAttempts = maxAttempts;
    this.expiresAt = expiresAt;
    Instant now = Instant.now();
    this.requestedAt = now;
    this.createdAt = now;
  }

  protected OtpCode() {}

  /** Records a failed verification attempt. */
  public void recordAttempt() {
    this.attempts += 1;
  }

  /** Marks the code as consumed, invalidating further verification. */
  public void consume() {
    this.consumedAt = Instant.now();
  }

  /** Returns true when the code is still usable. */
  public boolean isUsable() {
    return consumedAt == null && attempts < maxAttempts && !expiresAt.isBefore(Instant.now());
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

  public String getPurpose() {
    return purpose;
  }

  public String getCodeHash() {
    return codeHash;
  }

  public int getAttempts() {
    return attempts;
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getConsumedAt() {
    return consumedAt;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setId(UUID id) {
    this.id = id;
  }
}
