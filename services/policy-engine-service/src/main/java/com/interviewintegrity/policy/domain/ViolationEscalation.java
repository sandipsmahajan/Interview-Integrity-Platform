package com.interviewintegrity.policy.domain;

import com.interviewintegrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;

/** Escalation of a violation to a senior reviewer. */
public class ViolationEscalation {

  private UUID id;

  private UUID organizationId;

  private UUID violationId;

  private UUID escalatedTo;

  private String reason;

  private UUID escalatedBy;

  private Instant escalatedAt;

  private Instant resolvedAt;

  private String resolution;

  /** Creates a new escalation. */
  public ViolationEscalation(
      UUID organizationId, UUID violationId, UUID escalatedTo, String reason, UUID escalatedBy) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(violationId, "violationId");
    Assert.notNull(escalatedTo, "escalatedTo");
    this.organizationId = organizationId;
    this.violationId = violationId;
    this.escalatedTo = escalatedTo;
    this.reason = reason;
    this.escalatedBy = escalatedBy;
    this.escalatedAt = Instant.now();
  }

  /** Creates an escalation from a persisted row (row mapping). */
  public ViolationEscalation(
      UUID id,
      UUID organizationId,
      UUID violationId,
      UUID escalatedTo,
      String reason,
      UUID escalatedBy,
      Instant escalatedAt,
      Instant resolvedAt,
      String resolution) {
    this.id = id;
    this.organizationId = organizationId;
    this.violationId = violationId;
    this.escalatedTo = escalatedTo;
    this.reason = reason;
    this.escalatedBy = escalatedBy;
    this.escalatedAt = escalatedAt;
    this.resolvedAt = resolvedAt;
    this.resolution = resolution;
  }

  protected ViolationEscalation() {}

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getViolationId() {
    return violationId;
  }

  public UUID getEscalatedTo() {
    return escalatedTo;
  }

  public String getReason() {
    return reason;
  }

  public UUID getEscalatedBy() {
    return escalatedBy;
  }

  public Instant getEscalatedAt() {
    return escalatedAt;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public String getResolution() {
    return resolution;
  }
}
