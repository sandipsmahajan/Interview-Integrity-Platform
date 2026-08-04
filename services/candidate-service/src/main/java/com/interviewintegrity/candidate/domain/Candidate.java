package com.interviewintegrity.candidate.domain;

import com.interviewintegrity.validation.Assert;
import org.springframework.data.domain.Persistable;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Candidate master record; the root of the candidate service aggregate. */
@Table("candidates")
public class Candidate implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("user_id")
  private UUID userId;

  private String email;

  @Column("full_name")
  private String fullName;

  private String phone;

  private CandidateStatus status;

  private String source;

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

  /** Creates a new candidate in the NEW state. */
  public Candidate(
      UUID organizationId,
      UUID userId,
      String email,
      String fullName,
      String phone,
      String source,
      UUID createdBy) {
    Assert.notBlank(email, "email");
    Assert.notBlank(fullName, "fullName");
    this.organizationId = organizationId;
    this.userId = userId;
    this.email = email;
    this.fullName = fullName;
    this.phone = phone;
    this.source = source;
    this.createdBy = createdBy;
    this.status = CandidateStatus.NEW;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected Candidate() {}

  /** Replaces the mutable contact fields of the candidate. */
  public void update(String fullName, String phone, String source, UUID byUser) {
    Assert.notBlank(fullName, "fullName");
    this.fullName = fullName;
    this.phone = phone;
    this.source = source;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Moves the candidate into another lifecycle status. */
  public void changeStatus(CandidateStatus status, UUID byUser) {
    this.status = status;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Marks the candidate as soft deleted. */
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

  public UUID getUserId() {
    return userId;
  }

  public String getEmail() {
    return email;
  }

  public String getFullName() {
    return fullName;
  }

  public String getPhone() {
    return phone;
  }

  public CandidateStatus getStatus() {
    return status;
  }

  public String getSource() {
    return source;
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
