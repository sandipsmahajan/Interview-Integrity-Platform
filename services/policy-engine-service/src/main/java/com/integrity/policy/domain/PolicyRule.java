package com.integrity.policy.domain;

import com.integrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;

/** An evaluable rule belonging to a policy. The condition is a JSONB predicate. */
public class PolicyRule {

  private UUID id;

  private UUID organizationId;

  private UUID policyId;

  private String ruleCode;

  private String description;

  private String condition;

  private ViolationSeverity severity;

  private int weight;

  private int orderIndex;

  private boolean enabled;

  private UUID createdBy;

  private Instant createdAt;

  private UUID updatedBy;

  private Instant updatedAt;

  private UUID deletedBy;

  private Instant deletedAt;

  private long version;

  /** Creates a new rule for a policy. */
  public PolicyRule(
      UUID organizationId,
      UUID policyId,
      String ruleCode,
      String description,
      String condition,
      ViolationSeverity severity,
      Integer weight,
      Integer orderIndex,
      UUID createdBy) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(policyId, "policyId");
    Assert.notBlank(ruleCode, "ruleCode");
    Assert.notBlank(condition, "condition");
    this.organizationId = organizationId;
    this.policyId = policyId;
    this.ruleCode = ruleCode;
    this.description = description;
    this.condition = condition;
    this.severity = severity == null ? ViolationSeverity.MEDIUM : severity;
    this.weight = weight == null ? 1 : weight;
    this.orderIndex = orderIndex == null ? 0 : orderIndex;
    this.enabled = true;
    this.createdBy = createdBy;
    this.version = 1;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  /** Creates a rule from a persisted row (row mapping). */
  public PolicyRule(
      UUID id,
      UUID organizationId,
      UUID policyId,
      String ruleCode,
      String description,
      String condition,
      ViolationSeverity severity,
      int weight,
      int orderIndex,
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
    this.policyId = policyId;
    this.ruleCode = ruleCode;
    this.description = description;
    this.condition = condition;
    this.severity = severity;
    this.weight = weight;
    this.orderIndex = orderIndex;
    this.enabled = enabled;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
    this.updatedBy = updatedBy;
    this.updatedAt = updatedAt;
    this.deletedBy = deletedBy;
    this.deletedAt = deletedAt;
    this.version = version;
  }

  protected PolicyRule() {}

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getPolicyId() {
    return policyId;
  }

  public String getRuleCode() {
    return ruleCode;
  }

  public String getDescription() {
    return description;
  }

  public String getCondition() {
    return condition;
  }

  public ViolationSeverity getSeverity() {
    return severity;
  }

  public int getWeight() {
    return weight;
  }

  public int getOrderIndex() {
    return orderIndex;
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
