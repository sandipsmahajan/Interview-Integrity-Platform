package com.interviewintegrity.recruiter.domain;

import com.interviewintegrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Private note attached to a candidate by a recruiter. */
@Table("recruiter_notes")
public class RecruiterNote {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("recruiter_id")
  private UUID recruiterId;

  @Column("candidate_id")
  private UUID candidateId;

  private String body;

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

  /** Creates a note for a candidate. */
  public RecruiterNote(
      UUID organizationId, UUID recruiterId, UUID candidateId, String body, UUID createdBy) {
    Assert.notBlank(body, "body");
    this.organizationId = organizationId;
    this.recruiterId = recruiterId;
    this.candidateId = candidateId;
    this.body = body;
    this.createdBy = createdBy;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected RecruiterNote() {}

  /** Replaces the note body. */
  public void update(String body, UUID byUser) {
    Assert.notBlank(body, "body");
    this.body = body;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Marks the note as soft deleted. */
  public void delete(UUID byUser) {
    this.deletedBy = byUser;
    this.deletedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getRecruiterId() {
    return recruiterId;
  }

  public UUID getCandidateId() {
    return candidateId;
  }

  public String getBody() {
    return body;
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
