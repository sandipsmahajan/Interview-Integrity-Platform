package com.integrity.telemetry.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Read model of a single raw telemetry event from the {@code telemetry_events} partitioned table.
 *
 * <p>The payload is kept as a JSON text string so it can be exposed by the API without coupling to
 * the storage JSONB encoding.
 */
public class TelemetryEvent {

  private UUID id;

  private UUID organizationId;

  private UUID sessionId;

  private UUID interviewId;

  private String eventType;

  private long seq;

  private Instant occurredAt;

  private Instant clientOccurredAt;

  private String payload;

  /** Creates an event row model from a persisted row (row mapping). */
  public TelemetryEvent(
      UUID id,
      UUID organizationId,
      UUID sessionId,
      UUID interviewId,
      String eventType,
      long seq,
      Instant occurredAt,
      Instant clientOccurredAt,
      String payload) {
    this.id = id;
    this.organizationId = organizationId;
    this.sessionId = sessionId;
    this.interviewId = interviewId;
    this.eventType = eventType;
    this.seq = seq;
    this.occurredAt = occurredAt;
    this.clientOccurredAt = clientOccurredAt;
    this.payload = payload;
  }

  protected TelemetryEvent() {}

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

  public String getEventType() {
    return eventType;
  }

  public long getSeq() {
    return seq;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public Instant getClientOccurredAt() {
    return clientOccurredAt;
  }

  public String getPayload() {
    return payload;
  }
}
