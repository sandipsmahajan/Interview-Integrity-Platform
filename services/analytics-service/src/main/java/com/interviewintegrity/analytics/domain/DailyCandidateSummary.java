package com.interviewintegrity.analytics.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Per-candidate daily summary. */
public class DailyCandidateSummary {

  private LocalDate summaryDate;

  private UUID organizationId;

  private UUID candidateId;

  private long interviewsAttended;

  private BigDecimal avgScore;

  private long assessmentsCompleted;

  private long violations;

  private Instant createdAt;

  private Instant updatedAt;

  /** Creates a new zeroed summary for a candidate and date. */
  public DailyCandidateSummary(UUID organizationId, UUID candidateId, LocalDate summaryDate) {
    this.summaryDate = summaryDate;
    this.organizationId = organizationId;
    this.candidateId = candidateId;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  /** Creates a summary with explicit counters and timestamps (row mapping). */
  public DailyCandidateSummary(
      LocalDate summaryDate,
      UUID organizationId,
      UUID candidateId,
      long interviewsAttended,
      BigDecimal avgScore,
      long assessmentsCompleted,
      long violations,
      Instant createdAt,
      Instant updatedAt) {
    this.summaryDate = summaryDate;
    this.organizationId = organizationId;
    this.candidateId = candidateId;
    this.interviewsAttended = interviewsAttended;
    this.avgScore = avgScore;
    this.assessmentsCompleted = assessmentsCompleted;
    this.violations = violations;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  protected DailyCandidateSummary() {}

  /** Replaces the aggregated counters and refreshes the updated timestamp. */
  public void update(
      long interviewsAttended, BigDecimal avgScore, long assessmentsCompleted, long violations) {
    this.interviewsAttended = interviewsAttended;
    this.avgScore = avgScore;
    this.assessmentsCompleted = assessmentsCompleted;
    this.violations = violations;
    this.updatedAt = Instant.now();
  }

  public LocalDate getSummaryDate() {
    return summaryDate;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getCandidateId() {
    return candidateId;
  }

  public long getInterviewsAttended() {
    return interviewsAttended;
  }

  public BigDecimal getAvgScore() {
    return avgScore;
  }

  public long getAssessmentsCompleted() {
    return assessmentsCompleted;
  }

  public long getViolations() {
    return violations;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
