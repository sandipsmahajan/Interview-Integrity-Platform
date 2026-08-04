package com.interviewintegrity.analytics.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Observability log of an analytics aggregation run. */
@Table("analytics_job_runs")
public class AnalyticsJobRun implements Persistable<Long> {

  @Id private Long id;

  @Column("job_name")
  private String jobName;

  private String status;

  @Column("records_processed")
  private long recordsProcessed;

  @Column("error_message")
  private String errorMessage;

  @Column("started_at")
  private Instant startedAt;

  @Column("finished_at")
  private Instant finishedAt;

  @Column("duration_ms")
  private Long durationMs;

  /** Creates a new running job run. */
  public AnalyticsJobRun(String jobName) {
    this.jobName = jobName;
    this.status = JobRunStatus.RUNNING.name();
    this.startedAt = Instant.now();
  }

  protected AnalyticsJobRun() {}

  /** Marks the job run as successful. */
  public void succeed(long recordsProcessed) {
    this.status = JobRunStatus.SUCCEEDED.name();
    this.recordsProcessed = recordsProcessed;
    finish();
  }

  /** Marks the job run as failed. */
  public void fail(String errorMessage) {
    this.status = JobRunStatus.FAILED.name();
    this.errorMessage = errorMessage;
    finish();
  }

  private void finish() {
    this.finishedAt = Instant.now();
    this.durationMs = Math.max(0, ChronoUnit.MILLIS.between(startedAt, finishedAt));
  }

  @Override
  public Long getId() {
    return id;
  }

  public String getJobName() {
    return jobName;
  }

  public String getStatus() {
    return status;
  }

  public long getRecordsProcessed() {
    return recordsProcessed;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getFinishedAt() {
    return finishedAt;
  }

  public Long getDurationMs() {
    return durationMs;
  }

  public void setId(Long id) {
    this.id = id;
  }

  @Override
  public boolean isNew() {
    return this.id == null;
  }

  @Version
  private long version = 1;

  public long getVersion() {
    return version;
  }

}
