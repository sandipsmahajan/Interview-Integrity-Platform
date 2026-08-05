package com.integrity.candidate.domain;

import com.integrity.validation.Assert;
import io.r2dbc.postgresql.codec.Json;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Skill or behaviour assessment assigned to a candidate. */
@Table("assessments")
public class Assessment implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("candidate_id")
  private UUID candidateId;

  @Column("assessment_type")
  private String assessmentType;

  private AssessmentStatus status;

  private BigDecimal score;

  private Json metadata;

  @Column("assigned_by")
  private UUID assignedBy;

  @Column("assigned_at")
  private Instant assignedAt;

  @Column("started_at")
  private Instant startedAt;

  @Column("completed_at")
  private Instant completedAt;

  @Column("expires_at")
  private Instant expiresAt;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_at")
  private Instant updatedAt;

  @Version private long version = 1;

  @Override
  public boolean isNew() {
    return this.id == null;
  }

  /** Assigns an assessment to a candidate. */
  public Assessment(
      UUID organizationId,
      UUID candidateId,
      String assessmentType,
      UUID assignedBy,
      Instant expiresAt) {
    Assert.notBlank(assessmentType, "assessmentType");
    this.organizationId = organizationId;
    this.candidateId = candidateId;
    this.assessmentType = assessmentType;
    this.assignedBy = assignedBy;
    this.expiresAt = expiresAt;
    this.status = AssessmentStatus.ASSIGNED;
    this.metadata = Json.of("{}");
    Instant now = Instant.now();
    this.assignedAt = now;
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected Assessment() {}

  /** Starts the assessment. */
  public void start() {
    this.status = AssessmentStatus.IN_PROGRESS;
    this.startedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  /** Marks the assessment as completed with the given score. */
  public void complete(BigDecimal score) {
    Assert.isTrue(
        score == null
            || (score.compareTo(BigDecimal.ZERO) >= 0
                && score.compareTo(BigDecimal.valueOf(100)) <= 0),
        "score must be between 0 and 100");
    this.status = AssessmentStatus.COMPLETED;
    this.score = score;
    this.completedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  /** Expires the assessment without completion. */
  public void expire() {
    this.status = AssessmentStatus.EXPIRED;
    this.updatedAt = Instant.now();
  }

  @Override
  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getCandidateId() {
    return candidateId;
  }

  public String getAssessmentType() {
    return assessmentType;
  }

  public AssessmentStatus getStatus() {
    return status;
  }

  public BigDecimal getScore() {
    return score;
  }

  public String getMetadata() {
    return metadata == null ? "{}" : metadata.asString();
  }

  public UUID getAssignedBy() {
    return assignedBy;
  }

  public Instant getAssignedAt() {
    return assignedAt;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public long getVersion() {
    return version;
  }

  public void setId(UUID id) {
    this.id = id;
  }
}
