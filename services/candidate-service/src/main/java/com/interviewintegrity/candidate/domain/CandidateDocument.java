package com.interviewintegrity.candidate.domain;

import com.interviewintegrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Attachment uploaded for a candidate. */
@Table("candidate_documents")
public class CandidateDocument implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("candidate_id")
  private UUID candidateId;

  @Column("storage_object_id")
  private UUID storageObjectId;

  private String name;

  @Column("content_type")
  private String contentType;

  @Column("size_bytes")
  private long sizeBytes;

  @Column("uploaded_by")
  private UUID uploadedBy;

  @Column("uploaded_at")
  private Instant uploadedAt;

  @Column("deleted_by")
  private UUID deletedBy;

  @Column("deleted_at")
  private Instant deletedAt;

  @Version private long version = 1;

  @Override
  public boolean isNew() {
    return this.id == null;
  }

  /** Creates a document reference for an uploaded object. */
  public CandidateDocument(
      UUID organizationId,
      UUID candidateId,
      UUID storageObjectId,
      String name,
      String contentType,
      long sizeBytes,
      UUID uploadedBy) {
    Assert.notBlank(name, "name");
    Assert.isTrue(sizeBytes >= 0, "sizeBytes must be non-negative");
    this.organizationId = organizationId;
    this.candidateId = candidateId;
    this.storageObjectId = storageObjectId;
    this.name = name;
    this.contentType = contentType;
    this.sizeBytes = sizeBytes;
    this.uploadedBy = uploadedBy;
    this.uploadedAt = Instant.now();
  }

  protected CandidateDocument() {}

  /** Marks the document as soft deleted. */
  public void delete(UUID byUser) {
    this.deletedBy = byUser;
    this.deletedAt = Instant.now();
  }

  @Override public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getCandidateId() {
    return candidateId;
  }

  public UUID getStorageObjectId() {
    return storageObjectId;
  }

  public String getName() {
    return name;
  }

  public String getContentType() {
    return contentType;
  }

  public long getSizeBytes() {
    return sizeBytes;
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
