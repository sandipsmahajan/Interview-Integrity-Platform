package com.interviewintegrity.identity.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** A multi-factor authentication device registered to a user. */
@Table("mfa_devices")
public class MfaDevice implements Persistable<UUID> {

  @Id private UUID id;

  @Column("user_id")
  private UUID userId;

  @Column("organization_id")
  private UUID organizationId;

  private String kind;

  @Column("secret_ciphertext")
  private String secretCiphertext;

  @Column("verified_at")
  private Instant verifiedAt;

  @Column("last_used_at")
  private Instant lastUsedAt;

  @Column("created_by")
  private UUID createdBy;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_by")
  private UUID updatedBy;

  @Column("updated_at")
  private Instant updatedAt;

  @Column("deleted_by")
  private UUID deletedBy;

  @Column("deleted_at")
  private Instant deletedAt;

  @Version private long version = 1;

  /** Creates a new unverified MFA device. */
  public MfaDevice(UUID userId, UUID organizationId, String kind, String secretCiphertext) {
    this.userId = userId;
    this.organizationId = organizationId;
    this.kind = kind;
    this.secretCiphertext = secretCiphertext;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected MfaDevice() {}

  /** Marks the device as verified. */
  public void verify() {
    this.verifiedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  /** Marks the device as used for a successful challenge. */
  public void markUsed() {
    this.lastUsedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  /** Marks the device as soft deleted. */
  public void delete(UUID byUser) {
    this.deletedBy = byUser;
    this.deletedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  @Override
  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getKind() {
    return kind;
  }

  public String getSecretCiphertext() {
    return secretCiphertext;
  }

  public Instant getVerifiedAt() {
    return verifiedAt;
  }

  public Instant getLastUsedAt() {
    return lastUsedAt;
  }

  public UUID getCreatedBy() {
    return createdBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public UUID getUpdatedBy() {
    return updatedBy;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public UUID getDeletedBy() {
    return deletedBy;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public long getVersion() {
    return version;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  @Override
  public boolean isNew() {
    return this.id == null;
  }
}
