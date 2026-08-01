package com.interviewintegrity.analytics.web.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request to record a daily integrity summary.
 *
 * @param date summary date
 * @param totalEvents total proctoring events
 * @param violationsTotal total violations
 * @param violationsBySeverity JSONB severity counters
 * @param violationsByRule JSONB rule counters
 * @param sessionsStarted sessions started
 * @param sessionsAbandoned sessions abandoned
 * @param avgHeartbeatCadenceSeconds average heartbeat cadence
 */
public record RecordIntegritySummaryRequest(
    @NotNull LocalDate date,
    long totalEvents,
    long violationsTotal,
    String violationsBySeverity,
    String violationsByRule,
    long sessionsStarted,
    long sessionsAbandoned,
    BigDecimal avgHeartbeatCadenceSeconds) {}
