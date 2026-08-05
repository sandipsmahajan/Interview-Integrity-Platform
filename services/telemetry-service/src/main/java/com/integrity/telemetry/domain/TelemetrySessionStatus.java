package com.integrity.telemetry.domain;

/** Lifecycle state of a telemetry monitoring session. */
public enum TelemetrySessionStatus {
  /** Session created but not yet reporting events. */
  STARTED,
  /** Session actively receiving events from the client. */
  ACTIVE,
  /** Session ended cleanly. */
  ENDED,
  /** Session terminated without a clean end. */
  ABANDONED
}
