package com.interviewintegrity.organization.domain;

import com.interviewintegrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** The tenant root: every tenant-scoped row in the platform references this organization id. */
@Table("organizations")
public class Organization implements Persistable<UUID> {

  @Id private UUID id;

  private String name;

  private String slug;

  @Column("legal_name")
  private String legalName;

  private OrganizationStatus status;

  private String settings;

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

  /** Creates a new trial organization. */
  public Organization(String name, String slug, String legalName, String settings, UUID createdBy) {
    Assert.notBlank(name, "name");
    Assert.notBlank(slug, "slug");
    this.name = name;
    this.slug = slug;
    this.legalName = legalName;
    this.settings = settings == null ? "{}" : settings;
    this.createdBy = createdBy;
    this.status = OrganizationStatus.TRIAL;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected Organization() {}

  /** Moves the organization into the active state. */
  public void activate(UUID byUser) {
    this.status = OrganizationStatus.ACTIVE;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Suspends the organization. */
  public void suspend(UUID byUser) {
    this.status = OrganizationStatus.SUSPENDED;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Closes the organization permanently. */
  public void close(UUID byUser) {
    this.status = OrganizationStatus.CLOSED;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Updates the display and legal names. */
  public void rename(String name, String legalName, UUID byUser) {
    Assert.notBlank(name, "name");
    this.name = name;
    this.legalName = legalName;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Replaces the JSON settings blob. */
  public void updateSettings(String settings, UUID byUser) {
    this.settings = settings == null ? "{}" : settings;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Marks the organization as soft deleted. */
  public void delete(UUID byUser) {
    this.deletedBy = byUser;
    this.deletedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  @Override
  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getSlug() {
    return slug;
  }

  public String getLegalName() {
    return legalName;
  }

  public OrganizationStatus getStatus() {
    return status;
  }

  public String getSettings() {
    return settings;
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
