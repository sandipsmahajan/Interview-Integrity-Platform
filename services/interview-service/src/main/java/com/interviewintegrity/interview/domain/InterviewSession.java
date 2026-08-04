package com.interviewintegrity.interview.domain;

import com.interviewintegrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** One monitoring session per interview run. */
@Table("interview_sessions")
public class InterviewSession implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("interview_id")
  private UUID interviewId;

  @Column("session_token_hash")
  private String sessionTokenHash;

  @Column("device_id")
  private String deviceId;

  @Column("client_version")
  private String clientVersion;

  @Column("started_at")
  private Instant startedAt;

  @Column("ended_at")
  private Instant endedAt;

  private SessionStatus status;

  @Column("heartbeat_cadence_seconds")
  private int heartbeatCadenceSeconds;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_at")
  private Instant updatedAt;

  @Version private long version = 1;

  /** Creates a pending monitoring session for an interview. */
  public InterviewSession(
      UUID organizationId,
      UUID interviewId,
      String sessionTokenHash,
      String deviceId,
      String clientVersion,
      int heartbeatCadenceSeconds) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(interviewId, "interviewId");
    Assert.notBlank(sessionTokenHash, "sessionTokenHash");
    Assert.isTrue(
        heartbeatCadenceSeconds >= 1 && heartbeatCadenceSeconds <= 3600,
        "heartbeatCadenceSeconds must be between 1 and 3600");
    this.organizationId = organizationId;
    this.interviewId = interviewId;
    this.sessionTokenHash = sessionTokenHash;
    this.deviceId = deviceId;
    this.clientVersion = clientVersion;
    this.heartbeatCadenceSeconds = heartbeatCadenceSeconds;
    this.status = SessionStatus.PENDING;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected InterviewSession() {}

  /** Activates the session. */
  public void start() {
    Assert.isTrue(status == SessionStatus.PENDING, "Only pending sessions can be started");
    this.status = SessionStatus.ACTIVE;
    this.startedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  /** Suspends the active session. */
  public void pause() {
    Assert.isTrue(status == SessionStatus.ACTIVE, "Only active sessions can be paused");
    this.status = SessionStatus.PAUSED;
    this.updatedAt = Instant.now();
  }

  /** Resumes a paused session. */
  public void resume() {
    Assert.isTrue(status == SessionStatus.PAUSED, "Only paused sessions can be resumed");
    this.status = SessionStatus.ACTIVE;
    this.updatedAt = Instant.now();
  }

  /** Ends the session normally. */
  public void complete() {
    Assert.isTrue(
        status == SessionStatus.ACTIVE || status == SessionStatus.PAUSED,
        "Only active or paused sessions can be completed");
    this.status = SessionStatus.ENDED;
    this.endedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  /** Marks the session as ended abnormally. */
  public void markAbnormal() {
    Assert.isTrue(
        status == SessionStatus.ACTIVE || status == SessionStatus.PAUSED,
        "Only active or paused sessions can be marked abnormal");
    this.status = SessionStatus.ABNORMAL;
    this.endedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  @Override
  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getInterviewId() {
    return interviewId;
  }

  public String getSessionTokenHash() {
    return sessionTokenHash;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public String getClientVersion() {
    return clientVersion;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getEndedAt() {
    return endedAt;
  }

  public SessionStatus getStatus() {
    return status;
  }

  public int getHeartbeatCadenceSeconds() {
    return heartbeatCadenceSeconds;
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

  @Override
  public boolean isNew() {
    return this.id == null;
  }
}
