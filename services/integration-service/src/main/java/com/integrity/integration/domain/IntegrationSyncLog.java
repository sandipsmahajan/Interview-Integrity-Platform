package com.integrity.integration.domain;

import com.integrity.validation.Assert;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Sync run history of a connection. */
@Table("integration_sync_logs")
public class IntegrationSyncLog implements Persistable<Long> {

  @Id private Long id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("connection_id")
  private UUID connectionId;

  private SyncDirection direction;

  private SyncStatus status;

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

  /** Starts a new sync run for a connection. */
  public IntegrationSyncLog(UUID organizationId, UUID connectionId, SyncDirection direction) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(connectionId, "connectionId");
    Assert.notNull(direction, "direction");
    this.organizationId = organizationId;
    this.connectionId = connectionId;
    this.direction = direction;
    this.status = SyncStatus.RUNNING;
    this.startedAt = Instant.now();
  }

  protected IntegrationSyncLog() {}

  /** Completes the sync run successfully. */
  public void complete(long recordsProcessed) {
    this.status = SyncStatus.SUCCEEDED;
    this.recordsProcessed = recordsProcessed;
    this.finishedAt = Instant.now();
    this.durationMs = Duration.between(startedAt, finishedAt).toMillis();
  }

  /** Fails the sync run with an error detail. */
  public void fail(String errorMessage) {
    this.status = SyncStatus.FAILED;
    this.errorMessage = errorMessage;
    this.finishedAt = Instant.now();
    this.durationMs = Duration.between(startedAt, finishedAt).toMillis();
  }

  @Override
  public Long getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getConnectionId() {
    return connectionId;
  }

  public SyncDirection getDirection() {
    return direction;
  }

  public SyncStatus getStatus() {
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

  private long version = 1;

  public long getVersion() {
    return version;
  }

  @Override
  public boolean isNew() {
    return this.id == null;
  }
}
