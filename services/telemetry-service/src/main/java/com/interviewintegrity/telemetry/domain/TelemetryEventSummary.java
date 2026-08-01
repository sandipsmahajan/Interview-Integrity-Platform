package com.interviewintegrity.telemetry.domain;

import java.time.Instant;
import java.util.UUID;

/** Hourly rollup row for one session and event type. */
public class TelemetryEventSummary {

  private Instant bucketStart;

  private Instant bucketEnd;

  private UUID organizationId;

  private UUID sessionId;

  private String eventType;

  private long eventCount;

  private Long minSeq;

  private Long maxSeq;

  private String lastPayload;

  /** Creates a summary row from a persisted row (row mapping). */
  public TelemetryEventSummary(
      Instant bucketStart,
      Instant bucketEnd,
      UUID organizationId,
      UUID sessionId,
      String eventType,
      long eventCount,
      Long minSeq,
      Long maxSeq,
      String lastPayload) {
    this.bucketStart = bucketStart;
    this.bucketEnd = bucketEnd;
    this.organizationId = organizationId;
    this.sessionId = sessionId;
    this.eventType = eventType;
    this.eventCount = eventCount;
    this.minSeq = minSeq;
    this.maxSeq = maxSeq;
    this.lastPayload = lastPayload;
  }

  protected TelemetryEventSummary() {}

  public Instant getBucketStart() {
    return bucketStart;
  }

  public Instant getBucketEnd() {
    return bucketEnd;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getSessionId() {
    return sessionId;
  }

  public String getEventType() {
    return eventType;
  }

  public long getEventCount() {
    return eventCount;
  }

  public Long getMinSeq() {
    return minSeq;
  }

  public Long getMaxSeq() {
    return maxSeq;
  }

  public String getLastPayload() {
    return lastPayload;
  }
}
