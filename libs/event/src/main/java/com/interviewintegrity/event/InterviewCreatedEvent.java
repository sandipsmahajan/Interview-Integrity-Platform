package com.interviewintegrity.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when an interview is created.
 *
 * @param interviewId interview identifier
 * @param companyId owning company identifier
 * @param candidateId candidate identifier
 * @param recruiterId recruiter identifier
 * @param startsAt planned start instant
 * @param occurredAt instant of creation
 */
public record InterviewCreatedEvent(
    UUID interviewId,
    UUID companyId,
    UUID candidateId,
    UUID recruiterId,
    Instant startsAt,
    Instant occurredAt) {}
