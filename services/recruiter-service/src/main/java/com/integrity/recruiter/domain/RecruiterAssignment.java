package com.integrity.recruiter.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Explicit assignment of a candidate to a recruiter with a role. */
@Table("recruiter_assignments")
public class RecruiterAssignment implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("recruiter_id")
  private UUID recruiterId;

  @Column("candidate_id")
  private UUID candidateId;

  private String role;

  @Column("assigned_by")
  private UUID assignedBy;

  @Column("assigned_at")
  private Instant assignedAt;

  @Column("ended_at")
  private Instant endedAt;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_at")
  private Instant updatedAt;

  @Version private long version = 1;

  /** Creates an active assignment. */
  public RecruiterAssignment(
      UUID organizationId, UUID recruiterId, UUID candidateId, String role, UUID assignedBy) {
    this.organizationId = organizationId;
    this.recruiterId = recruiterId;
    this.candidateId = candidateId;
    this.role = role;
    this.assignedBy = assignedBy;
    Instant now = Instant.now();
    this.assignedAt = now;
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected RecruiterAssignment() {}

  /** Ends the assignment. */
  public void end() {
    this.endedAt = endedAt == null ? Instant.now() : endedAt;
    this.updatedAt = Instant.now();
  }

  /** Changes the assignment role. */
  public void changeRole(String role) {
    this.role = role;
    this.updatedAt = Instant.now();
  }

  @Override
  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getRecruiterId() {
    return recruiterId;
  }

  public UUID getCandidateId() {
    return candidateId;
  }

  public String getRole() {
    return role;
  }

  public UUID getAssignedBy() {
    return assignedBy;
  }

  public Instant getAssignedAt() {
    return assignedAt;
  }

  public Instant getEndedAt() {
    return endedAt;
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
