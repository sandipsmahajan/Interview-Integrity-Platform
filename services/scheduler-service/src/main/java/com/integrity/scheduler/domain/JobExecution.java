package com.integrity.scheduler.domain;

import com.integrity.validation.Assert;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.domain.Persistable;

/** Execution history of a job (1 job : N executions). */
@Table("job_executions")
public class JobExecution implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("job_id")
  private UUID jobId;

  private ExecutionStatus status;

  @Column("started_at")
  private Instant startedAt;

  @Column("finished_at")
  private Instant finishedAt;

  @Column("exit_code")
  private Integer exitCode;

  @Column("error_message")
  private String errorMessage;

  @Column("duration_ms")
  private Long durationMs;

  @Column("worker_id")
  private String workerId;

  @Column("created_at")
  private Instant createdAt;

  /** Starts a new execution for a job on a worker. */
  public JobExecution(UUID organizationId, UUID jobId, String workerId) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(jobId, "jobId");
    Assert.notBlank(workerId, "workerId");
    this.organizationId = organizationId;
    this.jobId = jobId;
    this.workerId = workerId;
    this.status = ExecutionStatus.RUNNING;
    Instant now = Instant.now();
    this.startedAt = now;
    this.createdAt = now;
  }

  protected JobExecution() {}

  /** Completes the execution successfully. */
  public void succeed(int exitCode) {
    this.status = ExecutionStatus.SUCCEEDED;
    this.exitCode = exitCode;
    this.finishedAt = Instant.now();
    this.durationMs = duration();
  }

  /** Fails the execution with an error detail. */
  public void fail(int exitCode, String errorMessage) {
    this.status = ExecutionStatus.FAILED;
    this.exitCode = exitCode;
    this.errorMessage = errorMessage;
    this.finishedAt = Instant.now();
    this.durationMs = duration();
  }

  /** Marks the execution as timed out. */
  public void timeOut() {
    this.status = ExecutionStatus.TIMED_OUT;
    this.finishedAt = Instant.now();
    this.durationMs = duration();
  }

  /** Marks the execution as skipped with a reason. */
  public void skip(String reason) {
    this.status = ExecutionStatus.SKIPPED;
    this.errorMessage = reason;
    this.finishedAt = Instant.now();
    this.durationMs = duration();
  }

  private Long duration() {
    return Duration.between(startedAt, finishedAt).toMillis();
  }

  @Override
  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getJobId() {
    return jobId;
  }

  public ExecutionStatus getStatus() {
    return status;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getFinishedAt() {
    return finishedAt;
  }

  public Integer getExitCode() {
    return exitCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public Long getDurationMs() {
    return durationMs;
  }

  public String getWorkerId() {
    return workerId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  private long version = 1;

  public long getVersion() {
    return version;
  }

  @Override
  public boolean isNew() {
    return this.id == null;
  }
}
