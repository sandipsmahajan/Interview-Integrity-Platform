package com.integrity.report.domain;

import com.integrity.validation.Assert;
import io.r2dbc.postgresql.codec.Json;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** The parameters used to generate a report, kept for reproducibility. */
@Table("report_requests")
public class ReportRequest implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("report_id")
  private UUID reportId;

  @Column("aggregation_level")
  private String aggregationLevel;

  @Column("time_range")
  private Json timeRange;

  private Json parameters;

  @Column("requested_by")
  private UUID requestedBy;

  @Column("requested_at")
  private Instant requestedAt;

  @Column("completed_at")
  private Instant completedAt;

  @Column("error_message")
  private String errorMessage;

  @Version private long version = 1;

  /** Creates a new report request. */
  public ReportRequest(
      UUID organizationId,
      UUID reportId,
      String aggregationLevel,
      String timeRange,
      String parameters,
      UUID requestedBy) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(reportId, "reportId");
    Assert.notBlank(aggregationLevel, "aggregationLevel");
    this.organizationId = organizationId;
    this.reportId = reportId;
    this.aggregationLevel = aggregationLevel;
    this.timeRange = Json.of(timeRange == null ? "{}" : timeRange);
    this.parameters = Json.of(parameters == null ? "{}" : parameters);
    this.requestedBy = requestedBy;
    this.requestedAt = Instant.now();
  }

  protected ReportRequest() {}

  /** Marks the request as completed successfully. */
  public void complete() {
    this.completedAt = Instant.now();
    String cleared = null;
    this.errorMessage = cleared;
  }

  /** Marks the request as failed with the given error. */
  public void fail(String errorMessage) {
    this.completedAt = Instant.now();
    this.errorMessage = errorMessage;
  }

  @Override
  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getReportId() {
    return reportId;
  }

  public String getAggregationLevel() {
    return aggregationLevel;
  }

  public String getTimeRange() {
    return timeRange == null ? "{}" : timeRange.asString();
  }

  public String getParameters() {
    return parameters == null ? "{}" : parameters.asString();
  }

  public UUID getRequestedBy() {
    return requestedBy;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public String getErrorMessage() {
    return errorMessage;
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
