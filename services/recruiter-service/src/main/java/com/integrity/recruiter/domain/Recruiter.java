package com.integrity.recruiter.domain;

import com.integrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Recruiter profile linking a user to recruiting activity within an organization. */
@Table("recruiters")
public class Recruiter implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("user_id")
  private UUID userId;

  @Column("full_name")
  private String fullName;

  private String email;

  private String title;

  private RecruiterStatus status;

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

  /** Creates a new active recruiter profile. */
  public Recruiter(
      UUID organizationId,
      UUID userId,
      String fullName,
      String email,
      String title,
      UUID createdBy) {
    Assert.notBlank(fullName, "fullName");
    Assert.notBlank(email, "email");
    Assert.notNull(userId, "userId");
    this.organizationId = organizationId;
    this.userId = userId;
    this.fullName = fullName;
    this.email = email;
    this.title = title;
    this.createdBy = createdBy;
    this.status = RecruiterStatus.ACTIVE;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected Recruiter() {}

  /** Updates the recruiter profile fields. */
  public void updateProfile(String fullName, String email, String title, UUID byUser) {
    Assert.notBlank(fullName, "fullName");
    Assert.notBlank(email, "email");
    this.fullName = fullName;
    this.email = email;
    this.title = title;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Changes the working status of the recruiter. */
  public void changeStatus(RecruiterStatus status, UUID byUser) {
    this.status = status;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Marks the recruiter profile as soft deleted. */
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

  public UUID getUserId() {
    return userId;
  }

  public String getFullName() {
    return fullName;
  }

  public String getEmail() {
    return email;
  }

  public String getTitle() {
    return title;
  }

  public RecruiterStatus getStatus() {
    return status;
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
