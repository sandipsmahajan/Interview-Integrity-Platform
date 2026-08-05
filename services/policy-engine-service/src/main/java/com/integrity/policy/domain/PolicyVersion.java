package com.integrity.policy.domain;

import com.integrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;

/** Immutable snapshot of a policy definition at a given version. */
public class PolicyVersion {

  private UUID id;

  private UUID organizationId;

  private UUID policyId;

  private int version;

  private String definition;

  private PolicyStatus status;

  private UUID publishedBy;

  private Instant publishedAt;

  private Instant createdAt;

  /** Creates a new policy version snapshot. */
  public PolicyVersion(
      UUID organizationId,
      UUID policyId,
      int version,
      String definition,
      PolicyStatus status,
      UUID publishedBy) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(policyId, "policyId");
    Assert.notBlank(definition, "definition");
    this.organizationId = organizationId;
    this.policyId = policyId;
    this.version = version;
    this.definition = definition;
    this.status = status == null ? PolicyStatus.DRAFT : status;
    this.publishedBy = publishedBy;
    this.publishedAt = Instant.now();
    this.createdAt = Instant.now();
  }

  /** Creates a policy version from a persisted row (row mapping). */
  public PolicyVersion(
      UUID id,
      UUID organizationId,
      UUID policyId,
      int version,
      String definition,
      PolicyStatus status,
      UUID publishedBy,
      Instant publishedAt,
      Instant createdAt) {
    this.id = id;
    this.organizationId = organizationId;
    this.policyId = policyId;
    this.version = version;
    this.definition = definition;
    this.status = status;
    this.publishedBy = publishedBy;
    this.publishedAt = publishedAt;
    this.createdAt = createdAt;
  }

  protected PolicyVersion() {}

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getPolicyId() {
    return policyId;
  }

  public int getVersion() {
    return version;
  }

  public String getDefinition() {
    return definition;
  }

  public PolicyStatus getStatus() {
    return status;
  }

  public UUID getPublishedBy() {
    return publishedBy;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
