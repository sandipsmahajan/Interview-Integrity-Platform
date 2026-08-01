package com.interviewintegrity.storage.domain;

import com.interviewintegrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Metadata of an object stored in the backing object store. */
@Table("storage_objects")
public class StorageObject {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("bucket_id")
  private UUID bucketId;

  private String key;

  @Column("size_bytes")
  private long sizeBytes;

  @Column("content_type")
  private String contentType;

  @Column("checksum_sha256")
  private String checksumSha256;

  @Column("storage_class")
  private StorageClass storageClass;

  @Column("storage_ref")
  private String storageRef;

  private String metadata;

  @Column("uploaded_by")
  private UUID uploadedBy;

  @Column("uploaded_at")
  private Instant uploadedAt;

  @Column("deleted_by")
  private UUID deletedBy;

  @Column("deleted_at")
  private Instant deletedAt;

  @Version private long version = 1;

  /** Creates the metadata entry for a stored object. */
  public StorageObject(
      UUID organizationId,
      UUID bucketId,
      String key,
      long sizeBytes,
      String contentType,
      String checksumSha256,
      StorageClass storageClass,
      String storageRef,
      String metadata,
      UUID uploadedBy) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(bucketId, "bucketId");
    Assert.notBlank(key, "key");
    Assert.isTrue(sizeBytes >= 0, "sizeBytes must not be negative");
    Assert.notBlank(storageRef, "storageRef");
    this.organizationId = organizationId;
    this.bucketId = bucketId;
    this.key = key;
    this.sizeBytes = sizeBytes;
    this.contentType = contentType;
    this.checksumSha256 = checksumSha256;
    this.storageClass = storageClass == null ? StorageClass.STANDARD : storageClass;
    this.storageRef = storageRef;
    this.metadata = metadata == null ? "{}" : metadata;
    this.uploadedBy = uploadedBy;
    this.uploadedAt = Instant.now();
  }

  protected StorageObject() {}

  /** Replaces the mutable metadata of the object. */
  public void update(String contentType, StorageClass storageClass, String metadata, UUID byUser) {
    this.contentType = contentType;
    this.storageClass = storageClass == null ? StorageClass.STANDARD : storageClass;
    this.metadata = metadata == null ? "{}" : metadata;
    this.uploadedBy = byUser;
    this.uploadedAt = Instant.now();
  }

  /** Marks the object as soft deleted. */
  public void delete(UUID byUser) {
    this.deletedBy = byUser;
    this.deletedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getBucketId() {
    return bucketId;
  }

  public String getKey() {
    return key;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public String getContentType() {
    return contentType;
  }

  public String getChecksumSha256() {
    return checksumSha256;
  }

  public StorageClass getStorageClass() {
    return storageClass;
  }

  public String getStorageRef() {
    return storageRef;
  }

  public String getMetadata() {
    return metadata;
  }

  public UUID getUploadedBy() {
    return uploadedBy;
  }

  public Instant getUploadedAt() {
    return uploadedAt;
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
}
