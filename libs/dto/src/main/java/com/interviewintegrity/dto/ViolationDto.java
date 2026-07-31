package com.interviewintegrity.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Cross-service representation of a detected integrity violation.
 *
 * @param id violation identifier
 * @param sessionId session the violation belongs to
 * @param ruleCode policy rule that triggered the violation
 * @param severity severity level
 * @param message human readable description
 * @param occurredAt instant the violation was detected
 */
public record ViolationDto(
    UUID id,
    UUID sessionId,
    String ruleCode,
    String severity,
    String message,
    Instant occurredAt) {}
