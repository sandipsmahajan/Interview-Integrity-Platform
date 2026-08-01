package com.interviewintegrity.policy.web.dto;

import com.interviewintegrity.policy.domain.ViolationSeverity;
import com.interviewintegrity.policy.domain.ViolationStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Public view of a detected violation.
 *
 * @param id identifier
 * @param sessionId telemetry session
 * @param interviewId monitored interview
 * @param policyId attributed policy, if any
 * @param ruleCode triggering rule
 * @param severity violation severity
 * @param message human readable description
 * @param status triage state
 * @param evidence JSON evidence
 * @param occurredAt instant the violation happened
 * @param detectedBy detector identity
 * @param createdAt creation instant
 */
public record ViolationResponse(
    UUID id,
    UUID sessionId,
    UUID interviewId,
    UUID policyId,
    String ruleCode,
    ViolationSeverity severity,
    String message,
    ViolationStatus status,
    String evidence,
    Instant occurredAt,
    String detectedBy,
    Instant createdAt) {}
