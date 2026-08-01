package com.interviewintegrity.recruiter.domain;

import com.interviewintegrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Configurable hiring pipeline stage within a tenant. */
@Table("pipeline_stages")
public class PipelineStage {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  private String code;
  private String name;

  @Column("order_index")
  private int orderIndex;

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

  /** Creates a pipeline stage at the given order position. */
  public PipelineStage(
      UUID organizationId, String code, String name, int orderIndex, UUID createdBy) {
    Assert.notBlank(code, "code");
    Assert.notBlank(name, "name");
    this.organizationId = organizationId;
    this.code = code;
    this.name = name;
    this.orderIndex = orderIndex;
    this.createdBy = createdBy;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected PipelineStage() {}

  /** Renames and reorders the stage. */
  public void update(String name, int orderIndex, UUID byUser) {
    Assert.notBlank(name, "name");
    this.name = name;
    this.orderIndex = orderIndex;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Marks the stage as soft deleted. */
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

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public int getOrderIndex() {
    return orderIndex;
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
