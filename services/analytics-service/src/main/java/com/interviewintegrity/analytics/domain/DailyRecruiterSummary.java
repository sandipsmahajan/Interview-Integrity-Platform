package com.interviewintegrity.analytics.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Per-recruiter daily performance summary. */
public class DailyRecruiterSummary {

  private LocalDate summaryDate;

  private UUID organizationId;

  private UUID recruiterId;

  private long interviewsHeld;

  private long interviewsCompleted;

  private long candidatesContacted;

  private BigDecimal avgFeedbackRating;

  private long violations;

  private Instant createdAt;

  private Instant updatedAt;

  /** Creates a new zeroed summary for a recruiter and date. */
  public DailyRecruiterSummary(UUID organizationId, UUID recruiterId, LocalDate summaryDate) {
    this.summaryDate = summaryDate;
    this.organizationId = organizationId;
    this.recruiterId = recruiterId;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  /** Creates a summary with explicit counters and timestamps (row mapping). */
  public DailyRecruiterSummary(
      LocalDate summaryDate,
      UUID organizationId,
      UUID recruiterId,
      long interviewsHeld,
      long interviewsCompleted,
      long candidatesContacted,
      BigDecimal avgFeedbackRating,
      long violations,
      Instant createdAt,
      Instant updatedAt) {
    this.summaryDate = summaryDate;
    this.organizationId = organizationId;
    this.recruiterId = recruiterId;
    this.interviewsHeld = interviewsHeld;
    this.interviewsCompleted = interviewsCompleted;
    this.candidatesContacted = candidatesContacted;
    this.avgFeedbackRating = avgFeedbackRating;
    this.violations = violations;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  protected DailyRecruiterSummary() {}

  /** Replaces the aggregated counters and refreshes the updated timestamp. */
  public void update(
      long interviewsHeld,
      long interviewsCompleted,
      long candidatesContacted,
      BigDecimal avgFeedbackRating,
      long violations) {
    this.interviewsHeld = interviewsHeld;
    this.interviewsCompleted = interviewsCompleted;
    this.candidatesContacted = candidatesContacted;
    this.avgFeedbackRating = avgFeedbackRating;
    this.violations = violations;
    this.updatedAt = Instant.now();
  }

  public LocalDate getSummaryDate() {
    return summaryDate;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getRecruiterId() {
    return recruiterId;
  }

  public long getInterviewsHeld() {
    return interviewsHeld;
  }

  public long getInterviewsCompleted() {
    return interviewsCompleted;
  }

  public long getCandidatesContacted() {
    return candidatesContacted;
  }

  public BigDecimal getAvgFeedbackRating() {
    return avgFeedbackRating;
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
