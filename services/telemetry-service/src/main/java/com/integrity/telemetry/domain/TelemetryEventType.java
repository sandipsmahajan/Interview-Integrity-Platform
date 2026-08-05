package com.integrity.telemetry.domain;

import java.time.Instant;
import java.util.UUID;

/** Catalog entry describing one telemetry event type and its retention policy. */
public class TelemetryEventType {

  private UUID id;

  private String code;

  private String name;

  private String description;

  private int retentionDays;

  private Instant createdAt;

  private Instant updatedAt;

  private long version;

  /** Creates an event type row from a persisted row (row mapping). */
  public TelemetryEventType(
      UUID id,
      String code,
      String name,
      String description,
      int retentionDays,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    this.id = id;
    this.code = code;
    this.name = name;
    this.description = description;
    this.retentionDays = retentionDays;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.version = version;
  }

  protected TelemetryEventType() {}

  public UUID getId() {
    return id;
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

  public int getRetentionDays() {
    return retentionDays;
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
}
