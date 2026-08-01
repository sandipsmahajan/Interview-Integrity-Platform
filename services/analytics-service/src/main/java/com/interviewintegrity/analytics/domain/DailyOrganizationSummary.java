package com.interviewintegrity.analytics.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Per-organization daily operational summary. */
public class DailyOrganizationSummary {

  private LocalDate summaryDate;

  private UUID organizationId;

  private long interviewsScheduled;

  private long interviewsCompleted;

  private long interviewsCancelled;

  private long candidatesActive;

  private long recruitersActive;

  private long violations;

  private BigDecimal avgIntegrityScore;

  private Instant createdAt;

  private Instant updatedAt;

  /** Creates a new zeroed summary for an organization and date. */
  public DailyOrganizationSummary(UUID organizationId, LocalDate summaryDate) {
    this.summaryDate = summaryDate;
    this.organizationId = organizationId;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  /** Creates a summary with explicit counters and timestamps (row mapping). */
  public DailyOrganizationSummary(
      LocalDate summaryDate,
      UUID organizationId,
      long interviewsScheduled,
      long interviewsCompleted,
      long interviewsCancelled,
      long candidatesActive,
      long recruitersActive,
      long violations,
      BigDecimal avgIntegrityScore,
      Instant createdAt,
      Instant updatedAt) {
    this.summaryDate = summaryDate;
    this.organizationId = organizationId;
    this.interviewsScheduled = interviewsScheduled;
    this.interviewsCompleted = interviewsCompleted;
    this.interviewsCancelled = interviewsCancelled;
    this.candidatesActive = candidatesActive;
    this.recruitersActive = recruitersActive;
    this.violations = violations;
    this.avgIntegrityScore = avgIntegrityScore;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  protected DailyOrganizationSummary() {}

  /** Replaces the aggregated counters and refreshes the updated timestamp. */
  public void update(
      long interviewsScheduled,
      long interviewsCompleted,
      long interviewsCancelled,
      long candidatesActive,
      long recruitersActive,
      long violations,
      BigDecimal avgIntegrityScore) {
    this.interviewsScheduled = interviewsScheduled;
    this.interviewsCompleted = interviewsCompleted;
    this.interviewsCancelled = interviewsCancelled;
    this.candidatesActive = candidatesActive;
    this.recruitersActive = recruitersActive;
    this.violations = violations;
    this.avgIntegrityScore = avgIntegrityScore;
    this.updatedAt = Instant.now();
  }

  public LocalDate getSummaryDate() {
    return summaryDate;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public long getInterviewsScheduled() {
    return interviewsScheduled;
  }

  public long getInterviewsCompleted() {
    return interviewsCompleted;
  }

  public long getInterviewsCancelled() {
    return interviewsCancelled;
  }

  public long getCandidatesActive() {
    return candidatesActive;
  }

  public long getRecruitersActive() {
    return recruitersActive;
  }

  public long getViolations() {
    return violations;
  }

  public BigDecimal getAvgIntegrityScore() {
    return avgIntegrityScore;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
