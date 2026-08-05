package com.integrity.configuration.domain;

import com.integrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Tenant scoped configuration value. */
@Table("configurations")
public class Configuration implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  private ConfigScope scope;

  private String key;

  private String value;

  private String description;

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

  /** Creates a tenant scoped configuration value. */
  public Configuration(
      UUID organizationId,
      ConfigScope scope,
      String key,
      String value,
      String description,
      UUID createdBy) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(scope, "scope");
    Assert.notBlank(key, "key");
    Assert.notBlank(value, "value");
    this.organizationId = organizationId;
    this.scope = scope;
    this.key = key;
    this.value = value;
    this.description = description;
    this.createdBy = createdBy;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected Configuration() {}

  /** Replaces the configuration value and description. */
  public void update(String value, String description, UUID byUser) {
    Assert.notBlank(value, "value");
    this.value = value;
    this.description = description;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Marks the configuration as soft deleted. */
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

  public ConfigScope getScope() {
    return scope;
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }

  public String getDescription() {
    return description;
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
