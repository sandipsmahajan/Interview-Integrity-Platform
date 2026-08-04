package com.interviewintegrity.audit.domain;

import com.interviewintegrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.domain.Persistable;

/** HTTP access log entry produced by the API gateway. */
@Table("api_audit_log")
public class ApiAuditLog implements Persistable<Long> {

  @Id private Long id;

  @Column("organization_id")
  private UUID organizationId;

  private String method;
  private String path;

  @Column("status_code")
  private int statusCode;

  @Column("duration_ms")
  private int durationMs;

  @Column("actor_id")
  private UUID actorId;

  @Column("request_id")
  private String requestId;

  @Column("client_ip")
  private String clientIp;

  @Column("occurred_at")
  private Instant occurredAt;

  /** Records an HTTP access log entry. */
  public ApiAuditLog(
      UUID organizationId,
      String method,
      String path,
      int statusCode,
      int durationMs,
      UUID actorId,
      String requestId,
      String clientIp,
      Instant occurredAt) {
    Assert.notBlank(method, "method");
    Assert.notBlank(path, "path");
    this.organizationId = organizationId;
    this.method = method;
    this.path = path;
    this.statusCode = statusCode;
    this.durationMs = durationMs;
    this.actorId = actorId;
    this.requestId = requestId;
    this.clientIp = clientIp;
    this.occurredAt = occurredAt == null ? Instant.now() : occurredAt;
  }

  protected ApiAuditLog() {}

  @Override
  public Long getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getMethod() {
    return method;
  }

  public String getPath() {
    return path;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public int getDurationMs() {
    return durationMs;
  }

  public UUID getActorId() {
    return actorId;
  }

  public String getRequestId() {
    return requestId;
  }

  public String getClientIp() {
    return clientIp;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setId(Long id) {
    this.id = id;
  }

  private long version = 1;

  public long getVersion() {
    return version;
  }

  @Override
  public boolean isNew() {
    return this.id == null;
  }
}
