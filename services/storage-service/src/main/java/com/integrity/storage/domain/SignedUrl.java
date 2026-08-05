package com.integrity.storage.domain;

import com.integrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Pre-signed URL grant; only the token hash is persisted. */
@Table("signed_urls")
public class SignedUrl implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("object_id")
  private UUID objectId;

  private UrlPurpose purpose;

  @Column("token_hash")
  private String tokenHash;

  @Column("expires_at")
  private Instant expiresAt;

  @Column("max_uses")
  private Integer maxUses;

  @Column("usage_count")
  private int usageCount;

  @Column("created_by")
  private UUID createdBy;

  @Column("created_at")
  private Instant createdAt;

  @Column("revoked_at")
  private Instant revokedAt;

  /** Creates a pre-signed URL grant. */
  public SignedUrl(
      UUID organizationId,
      UUID objectId,
      UrlPurpose purpose,
      String tokenHash,
      Instant expiresAt,
      Integer maxUses,
      UUID createdBy) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(objectId, "objectId");
    Assert.notNull(purpose, "purpose");
    Assert.notBlank(tokenHash, "tokenHash");
    Assert.notNull(expiresAt, "expiresAt");
    Assert.isTrue(maxUses == null || maxUses > 0, "maxUses must be positive");
    this.organizationId = organizationId;
    this.objectId = objectId;
    this.purpose = purpose;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
    this.maxUses = maxUses;
    this.usageCount = 0;
    this.createdBy = createdBy;
    this.createdAt = Instant.now();
  }

  protected SignedUrl() {}

  /** Revokes the grant, making it unusable immediately. */
  public void revoke(UUID byUser) {
    this.revokedAt = Instant.now();
    this.createdBy = byUser;
  }

  @Override
  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getObjectId() {
    return objectId;
  }

  public UrlPurpose getPurpose() {
    return purpose;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Integer getMaxUses() {
    return maxUses;
  }

  public int getUsageCount() {
    return usageCount;
  }

  public UUID getCreatedBy() {
    return createdBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  private long version = 1;

  public long getVersion() {
    return version;
  }

  @Override
  public boolean isNew() {
    return this.id == null;
  }
}
