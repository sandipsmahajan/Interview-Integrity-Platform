package com.interviewintegrity.interview.domain;

import com.interviewintegrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Interview master record within a tenant. */
@Table("interviews")
public class Interview implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("candidate_id")
  private UUID candidateId;

  @Column("recruiter_id")
  private UUID recruiterId;

  @Column("round_number")
  private int roundNumber;

  private String title;

  private InterviewStatus status;

  private InterviewMode mode;

  @Column("meeting_url")
  private String meetingUrl;

  @Column("starts_at")
  private Instant startsAt;

  @Column("ends_at")
  private Instant endsAt;

  private String timezone;

  private String metadata;

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

  /** Creates a new scheduled interview. */
  public Interview(
      UUID organizationId,
      UUID candidateId,
      UUID recruiterId,
      int roundNumber,
      String title,
      InterviewMode mode,
      String meetingUrl,
      Instant startsAt,
      Instant endsAt,
      String timezone,
      String metadata,
      UUID createdBy) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(candidateId, "candidateId");
    Assert.notNull(recruiterId, "recruiterId");
    Assert.notBlank(title, "title");
    Assert.isTrue(roundNumber > 0, "roundNumber must be positive");
    Assert.notNull(startsAt, "startsAt");
    Assert.notNull(endsAt, "endsAt");
    Assert.isTrue(endsAt.isAfter(startsAt), "endsAt must be after startsAt");
    this.organizationId = organizationId;
    this.candidateId = candidateId;
    this.recruiterId = recruiterId;
    this.roundNumber = roundNumber;
    this.title = title;
    this.mode = mode == null ? InterviewMode.ONLINE : mode;
    this.meetingUrl = meetingUrl;
    this.startsAt = startsAt;
    this.endsAt = endsAt;
    this.timezone = timezone == null ? "UTC" : timezone;
    this.metadata = metadata == null ? "{}" : metadata;
    this.createdBy = createdBy;
    this.status = InterviewStatus.SCHEDULED;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected Interview() {}

  /** Moves the interview into the in-progress state. */
  public void markInProgress(UUID byUser) {
    Assert.isTrue(
        status == InterviewStatus.SCHEDULED || status == InterviewStatus.IN_PROGRESS,
        "Only scheduled interviews can be started");
    this.status = InterviewStatus.IN_PROGRESS;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Completes the interview. */
  public void complete(UUID byUser) {
    Assert.isTrue(
        status == InterviewStatus.SCHEDULED || status == InterviewStatus.IN_PROGRESS,
        "Only scheduled or in-progress interviews can be completed");
    this.status = InterviewStatus.COMPLETED;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Cancels the interview before it has taken place. */
  public void cancel(UUID byUser) {
    Assert.isTrue(
        status == InterviewStatus.SCHEDULED || status == InterviewStatus.IN_PROGRESS,
        "Only scheduled or in-progress interviews can be cancelled");
    this.status = InterviewStatus.CANCELLED;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Records that the candidate did not attend. */
  public void markNoShow(UUID byUser) {
    Assert.isTrue(
        status == InterviewStatus.SCHEDULED || status == InterviewStatus.IN_PROGRESS,
        "Only scheduled or in-progress interviews can be marked no-show");
    this.status = InterviewStatus.NO_SHOW;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Re-schedules the interview timing and location. */
  public void schedule(
      Instant startsAt, Instant endsAt, String timezone, String meetingUrl, UUID byUser) {
    Assert.notNull(startsAt, "startsAt");
    Assert.notNull(endsAt, "endsAt");
    Assert.isTrue(endsAt.isAfter(startsAt), "endsAt must be after startsAt");
    this.startsAt = startsAt;
    this.endsAt = endsAt;
    this.timezone = timezone == null ? "UTC" : timezone;
    this.meetingUrl = meetingUrl;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Updates the mutable interview details. */
  public void update(String title, String meetingUrl, String metadata, UUID byUser) {
    Assert.notBlank(title, "title");
    this.title = title;
    this.meetingUrl = meetingUrl;
    this.metadata = metadata == null ? "{}" : metadata;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Marks the interview as soft deleted. */
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

  public UUID getCandidateId() {
    return candidateId;
  }

  public UUID getRecruiterId() {
    return recruiterId;
  }

  public int getRoundNumber() {
    return roundNumber;
  }

  public String getTitle() {
    return title;
  }

  public InterviewStatus getStatus() {
    return status;
  }

  public InterviewMode getMode() {
    return mode;
  }

  public String getMeetingUrl() {
    return meetingUrl;
  }

  public Instant getStartsAt() {
    return startsAt;
  }

  public Instant getEndsAt() {
    return endsAt;
  }

  public String getTimezone() {
    return timezone;
  }

  public String getMetadata() {
    return metadata;
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
