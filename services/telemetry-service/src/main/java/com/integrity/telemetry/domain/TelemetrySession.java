package com.integrity.telemetry.domain;

import com.integrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;

/**
 * A monitoring session master row for one interview.
 *
 * <p>Managed through the {@code telemetry_sessions} table. The interview id soft-references the
 * interview service and is not enforced by a foreign key.
 */
public class TelemetrySession {

  private UUID id;

  private UUID organizationId;

  private UUID interviewId;

  private UUID candidateId;

  private String deviceId;

  private String clientVersion;

  private TelemetrySessionStatus status;

  private int heartbeatCadenceSeconds;

  private Instant startedAt;

  private Instant endedAt;

  private Instant createdAt;

  private Instant updatedAt;

  private long version;

  /** Creates a new session in the {@link TelemetrySessionStatus#STARTED} state. */
  public TelemetrySession(
      UUID organizationId,
      UUID interviewId,
      UUID candidateId,
      String deviceId,
      String clientVersion,
      Integer heartbeatCadenceSeconds) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(interviewId, "interviewId");
    int cadence = heartbeatCadenceSeconds == null ? 5 : heartbeatCadenceSeconds;
    Assert.isTrue(
        cadence >= 1 && cadence <= 3600, "heartbeatCadenceSeconds must be between 1 and 3600");
    this.organizationId = organizationId;
    this.interviewId = interviewId;
    this.candidateId = candidateId;
    this.deviceId = deviceId;
    this.clientVersion = clientVersion;
    this.status = TelemetrySessionStatus.STARTED;
    this.heartbeatCadenceSeconds = cadence;
    Instant now = Instant.now();
    this.startedAt = now;
    this.createdAt = now;
    this.updatedAt = now;
    this.version = 1;
  }

  /** Creates a session from a persisted row (row mapping). */
  public TelemetrySession(
      UUID id,
      UUID organizationId,
      UUID interviewId,
      UUID candidateId,
      String deviceId,
      String clientVersion,
      TelemetrySessionStatus status,
      int heartbeatCadenceSeconds,
      Instant startedAt,
      Instant endedAt,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    this.id = id;
    this.organizationId = organizationId;
    this.interviewId = interviewId;
    this.candidateId = candidateId;
    this.deviceId = deviceId;
    this.clientVersion = clientVersion;
    this.status = status;
    this.heartbeatCadenceSeconds = heartbeatCadenceSeconds;
    this.startedAt = startedAt;
    this.endedAt = endedAt;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.version = version;
  }

  protected TelemetrySession() {}

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getInterviewId() {
    return interviewId;
  }

  public UUID getCandidateId() {
    return candidateId;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public String getClientVersion() {
    return clientVersion;
  }

  public TelemetrySessionStatus getStatus() {
    return status;
  }

  public int getHeartbeatCadenceSeconds() {
    return heartbeatCadenceSeconds;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getEndedAt() {
    return endedAt;
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

  /** Sets the identifier for sessions whose id is managed by the client. */
  public void setId(UUID id) {
    this.id = id;
  }
}
