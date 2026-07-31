package com.interviewintegrity.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when an interview session completes.
 *
 * @param interviewId interview identifier
 * @param sessionId session identifier
 * @param occurredAt instant of completion
 */
public record InterviewCompletedEvent(UUID interviewId, UUID sessionId, Instant occurredAt) {}
