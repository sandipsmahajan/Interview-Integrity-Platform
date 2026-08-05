package com.integrity.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Cross-service summary of an interview.
 *
 * @param id interview identifier
 * @param companyId owning company identifier
 * @param candidateId candidate identifier
 * @param recruiterId recruiter identifier
 * @param status current lifecycle status
 * @param startsAt planned start instant
 * @param endsAt planned end instant
 */
public record InterviewSummaryDto(
    UUID id,
    UUID companyId,
    UUID candidateId,
    UUID recruiterId,
    String status,
    Instant startsAt,
    Instant endsAt) {}
