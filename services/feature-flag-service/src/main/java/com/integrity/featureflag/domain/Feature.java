package com.integrity.featureflag.domain;

import com.integrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Feature catalog entry within a tenant. */
@Table("features")
public class Feature implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  private String code;
  private String name;
  private String description;
  private FlagKind kind;

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

  /** Creates a new feature in the catalog. */
  public Feature(
      UUID organizationId,
      String code,
      String name,
      String description,
      FlagKind kind,
      UUID createdBy) {
    Assert.notBlank(code, "code");
    Assert.notBlank(name, "name");
    this.organizationId = organizationId;
    this.code = code;
    this.name = name;
    this.description = description;
    this.kind = kind == null ? FlagKind.BOOLEAN : kind;
    this.createdBy = createdBy;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected Feature() {}

  /** Updates the feature display fields. */
  public void update(String name, String description, UUID byUser) {
    Assert.notBlank(name, "name");
    this.name = name;
    this.description = description;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Marks the feature as soft deleted. */
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

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public FlagKind getKind() {
    return kind;
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
