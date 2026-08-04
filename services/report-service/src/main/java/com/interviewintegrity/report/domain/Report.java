package com.interviewintegrity.report.domain;

import com.interviewintegrity.validation.Assert;
import io.r2dbc.postgresql.codec.Json;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** A report artifact owned by an organization. */
@Table("reports")
public class Report implements Persistable<UUID> {

  private static final int DEFAULT_RETENTION_DAYS = 30;

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  private ReportType type;

  private String title;

  private ReportStatus status;

  private ReportFormat format;

  private BigDecimal score;

  private Json filters;

  @Column("requested_by")
  private UUID requestedBy;

  @Column("updated_by")
  private UUID updatedBy;

  @Column("requested_at")
  private Instant requestedAt;

  @Column("generated_at")
  private Instant generatedAt;

  @Column("expires_at")
  private Instant expiresAt;

  @Column("storage_object_id")
  private UUID storageObjectId;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_at")
  private Instant updatedAt;

  @Version private long version = 1;

  /** Creates a new report request in the requested state. */
  public Report(
      UUID organizationId,
      ReportType type,
      String title,
      ReportFormat format,
      String filters,
      UUID requestedBy) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(type, "type");
    Assert.notBlank(title, "title");
    Assert.notNull(format, "format");
    this.organizationId = organizationId;
    this.type = type;
    this.title = title;
    this.format = format;
    this.filters = Json.of(filters == null ? "{}" : filters);
    this.requestedBy = requestedBy;
    this.status = ReportStatus.REQUESTED;
    Instant now = Instant.now();
    this.requestedAt = now;
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected Report() {}

  /** Resets the report for a new generation pass. */
  public void regenerate() {
    this.status = ReportStatus.GENERATING;
    Instant cleared = null;
    this.generatedAt = cleared;
    this.expiresAt = cleared;
    this.updatedAt = Instant.now();
  }

  /** Marks the report as generated and available for download. */
  public void complete() {
    this.status = ReportStatus.READY;
    Instant now = Instant.now();
    this.generatedAt = now;
    if (this.expiresAt == null) {
      this.expiresAt = now.plus(DEFAULT_RETENTION_DAYS, ChronoUnit.DAYS);
    }
    this.updatedAt = now;
  }

  /** Marks the report generation as failed. */
  public void fail() {
    this.status = ReportStatus.FAILED;
    this.updatedAt = Instant.now();
  }

  /** Expires the report so it is no longer downloadable. */
  public void expire() {
    this.status = ReportStatus.EXPIRED;
    this.updatedAt = Instant.now();
  }

  /** Attaches the storage object that holds the generated artifact. */
  public void attachStorage(UUID storageObjectId) {
    this.storageObjectId = storageObjectId;
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

  public String getTitle() {
    return title;
  }

  public ReportStatus getStatus() {
    return status;
  }

  public ReportFormat getFormat() {
    return format;
  }

  public BigDecimal getScore() {
    return score;
  }

  public String getFilters() {
    return filters == null ? "{}" : filters.asString();
  }

  public UUID getRequestedBy() {
    return requestedBy;
  }

  public UUID getUpdatedBy() {
    return updatedBy;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public UUID getStorageObjectId() {
    return storageObjectId;
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
