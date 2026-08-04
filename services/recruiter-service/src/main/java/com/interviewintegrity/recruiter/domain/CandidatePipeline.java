package com.interviewintegrity.recruiter.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Tracks candidate movement through the hiring pipeline stages. */
@Table("candidate_pipeline")
public class CandidatePipeline implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("candidate_id")
  private UUID candidateId;

  @Column("recruiter_id")
  private UUID recruiterId;

  @Column("stage_id")
  private UUID stageId;

  private int position;

  private PipelineStatus status;

  @Column("entered_at")
  private Instant enteredAt;

  @Column("exited_at")
  private Instant exitedAt;

  @Column("created_by")
  private UUID createdBy;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_at")
  private Instant updatedAt;

  @Version private long version = 1;

  /** Enters a candidate into a pipeline stage. */
  public CandidatePipeline(
      UUID organizationId,
      UUID candidateId,
      UUID recruiterId,
      UUID stageId,
      int position,
      UUID createdBy) {
    this.organizationId = organizationId;
    this.candidateId = candidateId;
    this.recruiterId = recruiterId;
    this.stageId = stageId;
    this.position = position;
    this.status = PipelineStatus.CURRENT;
    this.createdBy = createdBy;
    Instant now = Instant.now();
    this.enteredAt = now;
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected CandidatePipeline() {}

  /** Marks this pipeline entry as past and returns it. */
  public CandidatePipeline exit() {
    this.status = PipelineStatus.PAST;
    this.exitedAt = Instant.now();
    this.updatedAt = Instant.now();
    return this;
  }

  /** Updates the owning recruiter. */
  public void assignRecruiter(UUID recruiterId) {
    this.recruiterId = recruiterId;
    this.updatedAt = Instant.now();
  }

  /** Updates the position within the stage. */
  public void reposition(int position) {
    this.position = position;
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

  public UUID getRecruiterId() {
    return recruiterId;
  }

  public UUID getStageId() {
    return stageId;
  }

  public int getPosition() {
    return position;
  }

  public PipelineStatus getStatus() {
    return status;
  }

  public Instant getEnteredAt() {
    return enteredAt;
  }

  public Instant getExitedAt() {
    return exitedAt;
  }

  public UUID getCreatedBy() {
    return createdBy;
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

  @Override
  public boolean isNew() {
    return this.id == null;
  }
}
