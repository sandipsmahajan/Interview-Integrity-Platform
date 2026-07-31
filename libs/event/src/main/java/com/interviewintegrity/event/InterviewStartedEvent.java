package com.interviewintegrity.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when an interview session transitions into the in-progress state.
 *
 * @param interviewId interview identifier
 * @param sessionId session identifier
 * @param occurredAt instant of the transition
 */
public record InterviewStartedEvent(UUID interviewId, UUID sessionId, Instant occurredAt) {}
