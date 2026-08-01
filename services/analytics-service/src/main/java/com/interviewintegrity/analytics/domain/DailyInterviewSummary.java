package com.interviewintegrity.analytics.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Per-interview daily summary (one row per interview on its completion day). */
public class DailyInterviewSummary {

  private LocalDate summaryDate;

  private UUID organizationId;

  private UUID interviewId;

  private Integer durationMinutes;

  private BigDecimal integrityScore;

  private long violations;

  private String status;

  private Instant createdAt;

  private Instant updatedAt;

  /** Creates a new zeroed summary for an interview and date. */
  public DailyInterviewSummary(UUID organizationId, UUID interviewId, LocalDate summaryDate) {
    this.summaryDate = summaryDate;
    this.organizationId = organizationId;
    this.interviewId = interviewId;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  /** Creates a summary with explicit counters and timestamps (row mapping). */
  public DailyInterviewSummary(
      LocalDate summaryDate,
      UUID organizationId,
      UUID interviewId,
      Integer durationMinutes,
      BigDecimal integrityScore,
      long violations,
      String status,
      Instant createdAt,
      Instant updatedAt) {
    this.summaryDate = summaryDate;
    this.organizationId = organizationId;
    this.interviewId = interviewId;
    this.durationMinutes = durationMinutes;
    this.integrityScore = integrityScore;
    this.violations = violations;
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  protected DailyInterviewSummary() {}

  /** Replaces the aggregated counters and refreshes the updated timestamp. */
  public void update(
      Integer durationMinutes, BigDecimal integrityScore, long violations, String status) {
    this.durationMinutes = durationMinutes;
    this.integrityScore = integrityScore;
    this.violations = violations;
    this.status = status;
    this.updatedAt = Instant.now();
  }

  public LocalDate getSummaryDate() {
    return summaryDate;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getInterviewId() {
    return interviewId;
  }

  public Integer getDurationMinutes() {
    return durationMinutes;
  }

  public BigDecimal getIntegrityScore() {
    return integrityScore;
  }

  public long getViolations() {
    return violations;
  }

  public String getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
