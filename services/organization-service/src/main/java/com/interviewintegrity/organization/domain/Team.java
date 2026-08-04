package com.interviewintegrity.organization.domain;

import com.interviewintegrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Team grouping users within a department. */
@Table("teams")
public class Team implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("department_id")
  private UUID departmentId;

  private String name;

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

  /** Creates a team within the given department. */
  public Team(UUID organizationId, UUID departmentId, String name, UUID createdBy) {
    Assert.notBlank(name, "name");
    this.organizationId = organizationId;
    this.departmentId = departmentId;
    this.name = name;
    this.createdBy = createdBy;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected Team() {}

  /** Renames the team. */
  public void rename(String name, UUID byUser) {
    Assert.notBlank(name, "name");
    this.name = name;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Marks the team as soft deleted. */
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

  public UUID getDepartmentId() {
    return departmentId;
  }

  public String getName() {
    return name;
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
