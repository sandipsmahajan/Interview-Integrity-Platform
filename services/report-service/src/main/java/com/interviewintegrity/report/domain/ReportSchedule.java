package com.interviewintegrity.report.domain;

import com.interviewintegrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** A recurring report definition evaluated by the scheduler service. */
@Table("report_schedules")
public class ReportSchedule implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  private ReportType type;

  @Column("cron_expression")
  private String cronExpression;

  private ReportFormat format;

  private String recipients;

  private String parameters;

  private boolean enabled;

  @Column("next_run_at")
  private Instant nextRunAt;

  @Column("last_run_at")
  private Instant lastRunAt;

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

  /** Creates a new enabled report schedule. */
  public ReportSchedule(
      UUID organizationId,
      ReportType type,
      String cronExpression,
      ReportFormat format,
      String recipients,
      String parameters,
      Instant nextRunAt,
      UUID createdBy) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(type, "type");
    Assert.notBlank(cronExpression, "cronExpression");
    Assert.notNull(format, "format");
    this.organizationId = organizationId;
    this.type = type;
    this.cronExpression = cronExpression;
    this.format = format;
    this.recipients = recipients == null ? "[]" : recipients;
    this.parameters = parameters == null ? "{}" : parameters;
    this.nextRunAt = nextRunAt;
    this.createdBy = createdBy;
    this.enabled = true;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected ReportSchedule() {}

  /** Updates the schedule definition. */
  public void update(
      String cronExpression,
      ReportFormat format,
      String recipients,
      String parameters,
      Instant nextRunAt,
      UUID byUser) {
    Assert.notBlank(cronExpression, "cronExpression");
    Assert.notNull(format, "format");
    this.cronExpression = cronExpression;
    this.format = format;
    this.recipients = recipients == null ? "[]" : recipients;
    this.parameters = parameters == null ? "{}" : parameters;
    this.nextRunAt = nextRunAt;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Enables the schedule. */
  public void enable(UUID byUser) {
    this.enabled = true;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Disables the schedule. */
  public void disable(UUID byUser) {
    this.enabled = false;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Marks the schedule as soft deleted. */
  public void delete(UUID byUser) {
    this.enabled = false;
    this.deletedBy = byUser;
    this.deletedAt = Instant.now();
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  @Override
  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public ReportType getType() {
    return type;
  }

  public String getCronExpression() {
    return cronExpression;
  }

  public ReportFormat getFormat() {
    return format;
  }

  public String getRecipients() {
    return recipients;
  }

  public String getParameters() {
    return parameters;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public Instant getNextRunAt() {
    return nextRunAt;
  }

  public Instant getLastRunAt() {
    return lastRunAt;
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
