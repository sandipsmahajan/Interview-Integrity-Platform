package com.integrity.storage.domain;

import com.integrity.validation.Assert;
import io.r2dbc.postgresql.codec.Json;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Logical bucket owned by a tenant. */
@Table("storage_buckets")
public class StorageBucket implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  private String name;

  @Column("versioning_enabled")
  private boolean versioningEnabled;

  private Json policy;

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

  /** Creates a storage bucket. */
  public StorageBucket(
      UUID organizationId, String name, boolean versioningEnabled, String policy, UUID createdBy) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notBlank(name, "name");
    this.organizationId = organizationId;
    this.name = name;
    this.versioningEnabled = versioningEnabled;
    this.policy = Json.of(policy == null ? "{}" : policy);
    this.createdBy = createdBy;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected StorageBucket() {}

  /** Replaces the bucket configuration. */
  public void update(String name, boolean versioningEnabled, String policy, UUID byUser) {
    Assert.notBlank(name, "name");
    this.name = name;
    this.versioningEnabled = versioningEnabled;
    this.policy = Json.of(policy == null ? "{}" : policy);
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Marks the bucket as soft deleted. */
  public void delete(UUID byUser) {
    this.deletedBy = byUser;
    this.deletedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  @Override
  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getName() {
    return name;
  }

  public boolean isVersioningEnabled() {
    return versioningEnabled;
  }

  public String getPolicy() {
    return policy == null ? "{}" : policy.asString();
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
