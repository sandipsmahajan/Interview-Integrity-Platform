package com.integrity.analytics.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Per-organization daily integrity scorecard. */
public class DailyIntegritySummary {

  private LocalDate summaryDate;

  private UUID organizationId;

  private long totalEvents;

  private long violationsTotal;

  private String violationsBySeverity;

  private String violationsByRule;

  private long sessionsStarted;

  private long sessionsAbandoned;

  private BigDecimal avgHeartbeatCadenceSeconds;

  private Instant createdAt;

  private Instant updatedAt;

  /** Creates a new zeroed summary for an organization and date. */
  public DailyIntegritySummary(UUID organizationId, LocalDate summaryDate) {
    this.summaryDate = summaryDate;
    this.organizationId = organizationId;
    this.violationsBySeverity = "{}";
    this.violationsByRule = "{}";
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  /** Creates a summary with explicit counters and timestamps (row mapping). */
  public DailyIntegritySummary(
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
      Instant updatedAt) {
    this.summaryDate = summaryDate;
    this.organizationId = organizationId;
    this.totalEvents = totalEvents;
    this.violationsTotal = violationsTotal;
    this.violationsBySeverity = violationsBySeverity;
    this.violationsByRule = violationsByRule;
    this.sessionsStarted = sessionsStarted;
    this.sessionsAbandoned = sessionsAbandoned;
    this.avgHeartbeatCadenceSeconds = avgHeartbeatCadenceSeconds;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  protected DailyIntegritySummary() {}

  /** Replaces the aggregated counters and refreshes the updated timestamp. */
  public void update(
      long totalEvents,
      long violationsTotal,
      String violationsBySeverity,
      String violationsByRule,
      long sessionsStarted,
      long sessionsAbandoned,
      BigDecimal avgHeartbeatCadenceSeconds) {
    this.totalEvents = totalEvents;
    this.violationsTotal = violationsTotal;
    this.violationsBySeverity = violationsBySeverity;
    this.violationsByRule = violationsByRule;
    this.sessionsStarted = sessionsStarted;
    this.sessionsAbandoned = sessionsAbandoned;
    this.avgHeartbeatCadenceSeconds = avgHeartbeatCadenceSeconds;
    this.updatedAt = Instant.now();
  }

  public LocalDate getSummaryDate() {
    return summaryDate;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public long getTotalEvents() {
    return totalEvents;
  }

  public long getViolationsTotal() {
    return violationsTotal;
  }

  public String getViolationsBySeverity() {
    return violationsBySeverity;
  }

  public String getViolationsByRule() {
    return violationsByRule;
  }

  public long getSessionsStarted() {
    return sessionsStarted;
  }

  public long getSessionsAbandoned() {
    return sessionsAbandoned;
  }

  public BigDecimal getAvgHeartbeatCadenceSeconds() {
    return avgHeartbeatCadenceSeconds;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
