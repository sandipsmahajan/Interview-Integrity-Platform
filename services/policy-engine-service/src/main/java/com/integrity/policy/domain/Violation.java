package com.integrity.policy.domain;

import com.integrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;

/** A detected integrity violation for a telemetry session. */
public class Violation {

  private UUID id;

  private UUID organizationId;

  private UUID sessionId;

  private UUID interviewId;

  private UUID policyId;

  private String ruleCode;

  private ViolationSeverity severity;

  private String message;

  private ViolationStatus status;

  private String evidence;

  private Instant occurredAt;

  private String detectedBy;

  private Instant createdAt;

  private Instant updatedAt;

  private long version;

  /** Creates a new open violation. */
  public Violation(
      UUID organizationId,
      UUID sessionId,
      UUID interviewId,
      UUID policyId,
      String ruleCode,
      ViolationSeverity severity,
      String message,
      String evidence,
      Instant occurredAt,
      String detectedBy) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(sessionId, "sessionId");
    Assert.notBlank(ruleCode, "ruleCode");
    Assert.notNull(severity, "severity");
    Assert.notNull(occurredAt, "occurredAt");
    this.organizationId = organizationId;
    this.sessionId = sessionId;
    this.interviewId = interviewId;
    this.policyId = policyId;
    this.ruleCode = ruleCode;
    this.severity = severity;
    this.message = message;
    this.status = ViolationStatus.OPEN;
    this.evidence = evidence;
    this.occurredAt = occurredAt;
    this.detectedBy = detectedBy;
    this.version = 1;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  /** Creates a violation from a persisted row (row mapping). */
  public Violation(
      UUID id,
      UUID organizationId,
      UUID sessionId,
      UUID interviewId,
      UUID policyId,
      String ruleCode,
      ViolationSeverity severity,
      String message,
      ViolationStatus status,
      String evidence,
      Instant occurredAt,
      String detectedBy,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    this.id = id;
    this.organizationId = organizationId;
    this.sessionId = sessionId;
    this.interviewId = interviewId;
    this.policyId = policyId;
    this.ruleCode = ruleCode;
    this.severity = severity;
    this.message = message;
    this.status = status;
    this.evidence = evidence;
    this.occurredAt = occurredAt;
    this.detectedBy = detectedBy;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.version = version;
  }

  protected Violation() {}

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getSessionId() {
    return sessionId;
  }

  public UUID getInterviewId() {
    return interviewId;
  }

  public UUID getPolicyId() {
    return policyId;
  }

  public String getRuleCode() {
    return ruleCode;
  }

  public ViolationSeverity getSeverity() {
    return severity;
  }

  public String getMessage() {
    return message;
  }

  public ViolationStatus getStatus() {
    return status;
  }

  public String getEvidence() {
    return evidence;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public String getDetectedBy() {
    return detectedBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public long getVersion() {
    return version;
  }

  public void setId(UUID id) {
    this.id = id;
  }
}
