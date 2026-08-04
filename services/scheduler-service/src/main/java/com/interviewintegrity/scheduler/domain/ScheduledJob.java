package com.interviewintegrity.scheduler.domain;

import com.interviewintegrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** A scheduled job definition per tenant. */
@Table("scheduled_jobs")
public class ScheduledJob implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  private String name;

  @Column("job_type")
  private String jobType;

  @Column("cron_expression")
  private String cronExpression;

  private String handler;

  private String payload;

  private JobStatus status;

  @Column("max_retries")
  private int maxRetries;

  @Column("timeout_seconds")
  private int timeoutSeconds;

  @Column("retry_count")
  private int retryCount;

  @Column("last_run_at")
  private Instant lastRunAt;

  @Column("last_run_status")
  private ExecutionStatus lastRunStatus;

  @Column("next_run_at")
  private Instant nextRunAt;

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

  /** Creates a new enabled scheduled job. */
  public ScheduledJob(
      UUID organizationId,
      String name,
      String jobType,
      String cronExpression,
      String handler,
      String payload,
      int maxRetries,
      int timeoutSeconds,
      UUID createdBy) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notBlank(name, "name");
    Assert.notBlank(jobType, "jobType");
    Assert.notBlank(handler, "handler");
    Assert.isTrue(maxRetries >= 0, "maxRetries must be non-negative");
    Assert.isTrue(timeoutSeconds > 0, "timeoutSeconds must be positive");
    this.organizationId = organizationId;
    this.name = name;
    this.jobType = jobType;
    this.cronExpression = cronExpression;
    this.handler = handler;
    this.payload = payload;
    this.maxRetries = maxRetries;
    this.timeoutSeconds = timeoutSeconds;
    this.retryCount = 0;
    this.createdBy = createdBy;
    this.status = JobStatus.ENABLED;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected ScheduledJob() {}

  /** Updates the job definition. */
  public void update(
      String name,
      String cronExpression,
      String payload,
      int maxRetries,
      int timeoutSeconds,
      UUID updatedBy) {
    Assert.notBlank(name, "name");
    Assert.isTrue(maxRetries >= 0, "maxRetries must be non-negative");
    Assert.isTrue(timeoutSeconds > 0, "timeoutSeconds must be positive");
    this.name = name;
    this.cronExpression = cronExpression;
    this.payload = payload;
    this.maxRetries = maxRetries;
    this.timeoutSeconds = timeoutSeconds;
    this.updatedBy = updatedBy;
    this.updatedAt = Instant.now();
  }

  /** Pauses the job so it no longer triggers. */
  public void pause(UUID updatedBy) {
    this.status = JobStatus.PAUSED;
    this.updatedBy = updatedBy;
    this.updatedAt = Instant.now();
  }

  /** Resumes a paused job. */
  public void resume(UUID updatedBy) {
    this.status = JobStatus.ENABLED;
    this.updatedBy = updatedBy;
    this.updatedAt = Instant.now();
  }

  /** Disables the job. */
  public void disable(UUID updatedBy) {
    this.status = JobStatus.DISABLED;
    this.updatedBy = updatedBy;
    this.updatedAt = Instant.now();
  }

  /** Enables a disabled job. */
  public void enable(UUID updatedBy) {
    this.status = JobStatus.ENABLED;
    this.updatedBy = updatedBy;
    this.updatedAt = Instant.now();
  }

  /** Records a completed run and schedules the next one. */
  public void recordRun(ExecutionStatus runStatus, Instant nextRunAt) {
    Assert.notNull(runStatus, "runStatus");
    this.lastRunAt = Instant.now();
    this.lastRunStatus = runStatus;
    this.nextRunAt = nextRunAt;
    this.updatedAt = Instant.now();
  }

  /** Advances a due job, marking it run and clearing the next run. */
  public void advance(Instant nextRunAt) {
    this.lastRunAt = Instant.now();
    this.nextRunAt = nextRunAt;
    this.updatedAt = Instant.now();
  }

  /** Marks the job as soft deleted. */
  public void delete(UUID deletedBy) {
    this.deletedBy = deletedBy;
    this.deletedAt = Instant.now();
    this.updatedBy = deletedBy;
    this.updatedAt = Instant.now();
  }

  @Override
  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getName() {
    return name;
  }

  public String getJobType() {
    return jobType;
  }

  public String getCronExpression() {
    return cronExpression;
  }

  public String getHandler() {
    return handler;
  }

  public String getPayload() {
    return payload;
  }

  public JobStatus getStatus() {
    return status;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public int getTimeoutSeconds() {
    return timeoutSeconds;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public Instant getLastRunAt() {
    return lastRunAt;
  }

  public ExecutionStatus getLastRunStatus() {
    return lastRunStatus;
  }

  public Instant getNextRunAt() {
    return nextRunAt;
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
