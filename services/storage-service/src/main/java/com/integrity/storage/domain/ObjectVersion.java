package com.integrity.storage.domain;

import com.integrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Immutable version history entry of a storage object. */
@Table("object_versions")
public class ObjectVersion implements Persistable<Long> {

  @Id private Long id;

  @Column("object_id")
  private UUID objectId;

  @Column("organization_id")
  private UUID organizationId;

  private int version;

  @Column("storage_ref")
  private String storageRef;

  @Column("size_bytes")
  private long sizeBytes;

  @Column("checksum_sha256")
  private String checksumSha256;

  @Column("created_by")
  private UUID createdBy;

  @Column("created_at")
  private Instant createdAt;

  @Column("deleted_at")
  private Instant deletedAt;

  /** Creates an immutable object version record. */
  public ObjectVersion(
      UUID objectId,
      UUID organizationId,
      int version,
      String storageRef,
      long sizeBytes,
      String checksumSha256,
      UUID createdBy) {
    Assert.notNull(objectId, "objectId");
    Assert.notNull(organizationId, "organizationId");
    Assert.isTrue(version > 0, "version must be positive");
    Assert.notBlank(storageRef, "storageRef");
    Assert.isTrue(sizeBytes >= 0, "sizeBytes must not be negative");
    this.objectId = objectId;
    this.organizationId = organizationId;
    this.version = version;
    this.storageRef = storageRef;
    this.sizeBytes = sizeBytes;
    this.checksumSha256 = checksumSha256;
    this.createdBy = createdBy;
    this.createdAt = Instant.now();
  }

  protected ObjectVersion() {}

  @Override
  public Long getId() {
    return id;
  }

  public UUID getObjectId() {
    return objectId;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public int getVersion() {
    return version;
  }

  public String getStorageRef() {
    return storageRef;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public String getChecksumSha256() {
    return checksumSha256;
  }

  public UUID getCreatedBy() {
    return createdBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public void setId(Long id) {
    this.id = id;
  }

  @Override
  public boolean isNew() {
    return this.id == null;
  }
}
