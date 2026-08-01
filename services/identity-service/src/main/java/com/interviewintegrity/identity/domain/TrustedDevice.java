package com.interviewintegrity.identity.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** A device trusted to skip the MFA challenge after a successful verification. */
@Table("trusted_devices")
public class TrustedDevice {

  @Id private UUID id;

  @Column("user_id")
  private UUID userId;

  @Column("organization_id")
  private UUID organizationId;

  @Column("device_id")
  private String deviceId;

  @Column("device_name")
  private String deviceName;

  @Column("first_seen_at")
  private Instant firstSeenAt;

  @Column("last_seen_at")
  private Instant lastSeenAt;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_at")
  private Instant updatedAt;

  /** Creates a new trusted device record. */
  public TrustedDevice(UUID userId, UUID organizationId, String deviceId, String deviceName) {
    this.userId = userId;
    this.organizationId = organizationId;
    this.deviceId = deviceId;
    this.deviceName = deviceName;
    Instant now = Instant.now();
    this.firstSeenAt = now;
    this.lastSeenAt = now;
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected TrustedDevice() {}

  /** Refreshes the last seen timestamp. */
  public void touch() {
    this.lastSeenAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public String getDeviceName() {
    return deviceName;
  }

  public Instant getFirstSeenAt() {
    return firstSeenAt;
  }

  public Instant getLastSeenAt() {
    return lastSeenAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setId(UUID id) {
    this.id = id;
  }
}
