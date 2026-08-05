package com.integrity.candidate.domain;

import com.integrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Collaboration note attached to a candidate. */
@Table("candidate_notes")
public class CandidateNote implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("candidate_id")
  private UUID candidateId;

  @Column("author_id")
  private UUID authorId;

  private String body;

  private boolean pinned;

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

  @Override
  public boolean isNew() {
    return this.id == null;
  }

  /** Creates a note for a candidate. */
  public CandidateNote(
      UUID organizationId, UUID candidateId, UUID authorId, String body, UUID createdBy) {
    Assert.notBlank(body, "body");
    this.organizationId = organizationId;
    this.candidateId = candidateId;
    this.authorId = authorId;
    this.body = body;
    this.pinned = false;
    this.createdBy = createdBy;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected CandidateNote() {}

  /** Replaces the note body. */
  public void update(String body, UUID byUser) {
    Assert.notBlank(body, "body");
    this.body = body;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Pins or unpins the note. */
  public void setPinned(boolean pinned, UUID byUser) {
    this.pinned = pinned;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Marks the note as soft deleted. */
  public void delete(UUID byUser) {
    this.deletedBy = byUser;
    this.deletedAt = Instant.now();
    this.updatedAt = Instant.now();
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

  public UUID getAuthorId() {
    return authorId;
  }

  public String getBody() {
    return body;
  }

  public boolean isPinned() {
    return pinned;
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
}
