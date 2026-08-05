package com.integrity.telemetry.service;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload of the {@code policy.violation.v1} event published for client-side proctor alerts.
 *
 * <p>Field names are part of the cross-service contract consumed by the policy engine service.
 *
 * @param sessionId telemetry session that produced the alert
 * @param interviewId interview being monitored
 * @param policyId optional policy reference; null when not yet attributed
 * @param ruleCode rule that triggered (the alert event type for client alerts)
 * @param severity violation severity
 * @param message human readable alert description
 * @param evidence JSON evidence captured with the alert
 * @param occurredAt instant the alert was generated
 * @param detectedBy detector identity
 */
public record TelemetryViolationEvent(
    UUID sessionId,
    UUID interviewId,
    UUID policyId,
    String ruleCode,
    String severity,
    String message,
    String evidence,
    Instant occurredAt,
    String detectedBy) {}
