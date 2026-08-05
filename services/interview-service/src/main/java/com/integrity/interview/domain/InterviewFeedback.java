package com.integrity.interview.domain;

import com.integrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Structured feedback collected after an interview. */
@Table("interview_feedback")
public class InterviewFeedback implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("interview_id")
  private UUID interviewId;

  @Column("interviewer_id")
  private UUID interviewerId;

  private Integer rating;

  private String strengths;

  private String concerns;

  private String recommendation;

  private FeedbackStatus status;

  @Column("submitted_at")
  private Instant submittedAt;

  @Column("created_by")
  private UUID createdBy;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_by")
  private UUID updatedBy;

  @Column("updated_at")
  private Instant updatedAt;

  @Column("deleted_by")
  private UUID deletedBy;

  @Column("deleted_at")
  private Instant deletedAt;

  @Version private long version = 1;

  /** Creates an empty draft feedback record for an interviewer. */
  public InterviewFeedback(
      UUID organizationId, UUID interviewId, UUID interviewerId, UUID createdBy) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(interviewId, "interviewId");
    Assert.notNull(interviewerId, "interviewerId");
    this.organizationId = organizationId;
    this.interviewId = interviewId;
    this.interviewerId = interviewerId;
    this.createdBy = createdBy;
    this.status = FeedbackStatus.DRAFT;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected InterviewFeedback() {}

  /** Replaces the feedback body fields. */
  public void update(
      Integer rating, String strengths, String concerns, String recommendation, UUID byUser) {
    Assert.isTrue(rating == null || (rating >= 1 && rating <= 5), "rating must be between 1 and 5");
    this.rating = rating;
    this.strengths = strengths;
    this.concerns = concerns;
    this.recommendation = recommendation;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Finalises and submits the feedback. */
  public void submit(UUID byUser) {
    Assert.isTrue(status == FeedbackStatus.DRAFT, "Only draft feedback can be submitted");
    this.status = FeedbackStatus.SUBMITTED;
    this.submittedAt = Instant.now();
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Marks the feedback as soft deleted. */
  public void delete(UUID byUser) {
    this.deletedBy = byUser;
    this.deletedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  @Override
  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getInterviewId() {
    return interviewId;
  }

  public UUID getInterviewerId() {
    return interviewerId;
  }

  public Integer getRating() {
    return rating;
  }

  public String getStrengths() {
    return strengths;
  }

  public String getConcerns() {
    return concerns;
  }

  public String getRecommendation() {
    return recommendation;
  }

  public FeedbackStatus getStatus() {
    return status;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public UUID getCreatedBy() {
    return createdBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public UUID getUpdatedBy() {
    return updatedBy;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public UUID getDeletedBy() {
    return deletedBy;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public long getVersion() {
    return version;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  @Override
  public boolean isNew() {
    return this.id == null;
  }
}
