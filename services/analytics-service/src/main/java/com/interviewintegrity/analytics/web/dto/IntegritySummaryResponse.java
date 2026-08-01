package com.interviewintegrity.analytics.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Public view of a daily integrity summary.
 *
 * @param summaryDate summary date
 * @param organizationId owning tenant
 * @param totalEvents total proctoring events
 * @param violationsTotal total violations
 * @param violationsBySeverity JSONB severity counters
 * @param violationsByRule JSONB rule counters
 * @param sessionsStarted sessions started
 * @param sessionsAbandoned sessions abandoned
 * @param avgHeartbeatCadenceSeconds average heartbeat cadence
 * @param createdAt creation instant
 * @param updatedAt last update instant
 */
public record IntegritySummaryResponse(
    LocalDate summaryDate,
    UUID organizationId,
    long totalEvents,
    long violationsTotal,
    String violationsBySeverity,
    String violationsByRule,
    long sessionsStarted,
    long sessionsAbandoned,
    BigDecimal avgHeartbeatCadenceSeconds,
    Instant createdAt,
    Instant updatedAt) {}
