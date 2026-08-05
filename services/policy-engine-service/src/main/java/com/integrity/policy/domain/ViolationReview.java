package com.integrity.policy.domain;

import com.integrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;

/** Human review decision recorded against a violation. */
public class ViolationReview {

  private UUID id;

  private UUID organizationId;

  private UUID violationId;

  private UUID reviewerId;

  private ReviewAction action;

  private String comment;

  private Instant reviewedAt;

  /** Creates a new review record. */
  public ViolationReview(
      UUID organizationId, UUID violationId, UUID reviewerId, ReviewAction action, String comment) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(violationId, "violationId");
    Assert.notNull(reviewerId, "reviewerId");
    Assert.notNull(action, "action");
    this.organizationId = organizationId;
    this.violationId = violationId;
    this.reviewerId = reviewerId;
    this.action = action;
    this.comment = comment;
    this.reviewedAt = Instant.now();
  }

  /** Creates a review from a persisted row (row mapping). */
  public ViolationReview(
      UUID id,
      UUID organizationId,
      UUID violationId,
      UUID reviewerId,
      ReviewAction action,
      String comment,
      Instant reviewedAt) {
    this.id = id;
    this.organizationId = organizationId;
    this.violationId = violationId;
    this.reviewerId = reviewerId;
    this.action = action;
    this.comment = comment;
    this.reviewedAt = reviewedAt;
  }

  protected ViolationReview() {}

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getViolationId() {
    return violationId;
  }

  public UUID getReviewerId() {
    return reviewerId;
  }

  public ReviewAction getAction() {
    return action;
  }

  public String getComment() {
    return comment;
  }

  public Instant getReviewedAt() {
    return reviewedAt;
  }
}
