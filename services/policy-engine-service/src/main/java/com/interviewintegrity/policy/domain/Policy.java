package com.interviewintegrity.policy.domain;

import com.interviewintegrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;

/** A tenant scoped integrity policy grouping a set of evaluable rules. */
public class Policy {

  private UUID id;

  private UUID organizationId;

  private String code;

  private String name;

  private String description;

  private PolicyStatus status;

  private ViolationSeverity defaultSeverity;

  private int priority;

  private boolean enabled;

  private UUID createdBy;

  private Instant createdAt;

  private UUID updatedBy;

  private Instant updatedAt;

  private UUID deletedBy;

  private Instant deletedAt;

  private long version;

  /** Creates a new draft policy. */
  public Policy(
      UUID organizationId,
      String code,
      String name,
      String description,
      ViolationSeverity defaultSeverity,
      Integer priority,
      UUID createdBy) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notBlank(code, "code");
    Assert.notBlank(name, "name");
    this.organizationId = organizationId;
    this.code = code;
    this.name = name;
    this.description = description;
    this.status = PolicyStatus.DRAFT;
    this.defaultSeverity = defaultSeverity == null ? ViolationSeverity.MEDIUM : defaultSeverity;
    this.priority = priority == null ? 100 : priority;
    this.enabled = true;
    this.createdBy = createdBy;
    this.version = 1;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  /** Creates a policy from a persisted row (row mapping). */
  public Policy(
      UUID id,
      UUID organizationId,
      String code,
      String name,
      String description,
      PolicyStatus status,
      ViolationSeverity defaultSeverity,
      int priority,
      boolean enabled,
      UUID createdBy,
      Instant createdAt,
      UUID updatedBy,
      Instant updatedAt,
      UUID deletedBy,
      Instant deletedAt,
      long version) {
    this.id = id;
    this.organizationId = organizationId;
    this.code = code;
    this.name = name;
    this.description = description;
    this.status = status;
    this.defaultSeverity = defaultSeverity;
    this.priority = priority;
    this.enabled = enabled;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
    this.updatedBy = updatedBy;
    this.updatedAt = updatedAt;
    this.deletedBy = deletedBy;
    this.deletedAt = deletedAt;
    this.version = version;
  }

  protected Policy() {}

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

  public String getDescription() {
    return description;
  }

  public PolicyStatus getStatus() {
    return status;
  }

  public ViolationSeverity getDefaultSeverity() {
    return defaultSeverity;
  }

  public int getPriority() {
    return priority;
  }

  public boolean isEnabled() {
    return enabled;
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
