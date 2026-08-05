package com.integrity.storage.domain;

import com.integrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Snapshot of a storage object, populated by a database trigger. */
@Table("storage_objects_history")
public class StorageObjectHistory implements Persistable<UUID> {

  @Id
  @Column("history_id")
  private Long historyId;

  @Column("history_action")
  private String historyAction;

  @Column("changed_by")
  private UUID changedBy;

  @Column("changed_at")
  private Instant changedAt;

  private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("bucket_id")
  private UUID bucketId;

  private String key;

  @Column("size_bytes")
  private Long sizeBytes;

  @Column("content_type")
  private String contentType;

  @Column("storage_class")
  private StorageClass storageClass;

  private Long version;

  /** Creates a storage object history snapshot. */
  public StorageObjectHistory(
      String historyAction,
      UUID changedBy,
      Instant changedAt,
      UUID id,
      UUID organizationId,
      UUID bucketId,
      String key,
      Long sizeBytes,
      String contentType,
      StorageClass storageClass,
      Long version) {
    Assert.notBlank(historyAction, "historyAction");
    this.historyAction = historyAction;
    this.changedBy = changedBy;
    this.changedAt = changedAt;
    this.id = id;
    this.organizationId = organizationId;
    this.bucketId = bucketId;
    this.key = key;
    this.sizeBytes = sizeBytes;
    this.contentType = contentType;
    this.storageClass = storageClass;
    this.version = version;
  }

  protected StorageObjectHistory() {}

  public Long getHistoryId() {
    return historyId;
  }

  public String getHistoryAction() {
    return historyAction;
  }

  public UUID getChangedBy() {
    return changedBy;
  }

  public Instant getChangedAt() {
    return changedAt;
  }

  @Override
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

  public Long getSizeBytes() {
    return sizeBytes;
  }

  public String getContentType() {
    return contentType;
  }

  public StorageClass getStorageClass() {
    return storageClass;
  }

  public Long getVersion() {
    return version;
  }

  public void setHistoryId(Long historyId) {
    this.historyId = historyId;
  }

  @Override
  public boolean isNew() {
    return this.id == null;
  }
}
