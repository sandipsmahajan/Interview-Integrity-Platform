package com.interviewintegrity.audit.domain;

import com.interviewintegrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Append-only compliance audit event. */
@Table("audit_events")
public class AuditEvent {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("actor_id")
  private UUID actorId;

  @Column("actor_type")
  private String actorType;

  private String action;

  @Column("resource_type")
  private String resourceType;

  @Column("resource_id")
  private UUID resourceId;

  private AuditOutcome outcome;

  @Column("occurred_at")
  private Instant occurredAt;

  @Column("request_id")
  private String requestId;

  @Column("ip_address")
  private String ipAddress;

  @Column("user_agent")
  private String userAgent;

  private String metadata;

  /** Records a new compliance audit event. */
  public AuditEvent(
      UUID organizationId,
      UUID actorId,
      String actorType,
      String action,
      String resourceType,
      UUID resourceId,
      AuditOutcome outcome,
      Instant occurredAt,
      String requestId,
      String ipAddress,
      String userAgent,
      String metadata) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notBlank(action, "action");
    Assert.notBlank(resourceType, "resourceType");
    Assert.notNull(occurredAt, "occurredAt");
    this.organizationId = organizationId;
    this.actorId = actorId;
    this.actorType = actorType == null ? "USER" : actorType;
    this.action = action;
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.outcome = outcome == null ? AuditOutcome.SUCCESS : outcome;
    this.occurredAt = occurredAt;
    this.requestId = requestId;
    this.ipAddress = ipAddress;
    this.userAgent = userAgent;
    this.metadata = metadata == null ? "{}" : metadata;
  }

  protected AuditEvent() {}

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getActorId() {
    return actorId;
  }

  public String getActorType() {
    return actorType;
  }

  public String getAction() {
    return action;
  }

  public String getResourceType() {
    return resourceType;
  }

  public UUID getResourceId() {
    return resourceId;
  }

  public AuditOutcome getOutcome() {
    return outcome;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public String getRequestId() {
    return requestId;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public String getMetadata() {
    return metadata;
  }

  public void setId(UUID id) {
    this.id = id;
  }
}
