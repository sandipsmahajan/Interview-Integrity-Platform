package com.interviewintegrity.report.domain;

import com.interviewintegrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** An ordered section within a generated report. */
@Table("report_sections")
public class ReportSection {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("report_id")
  private UUID reportId;

  @Column("section_type")
  private String sectionType;

  private String title;

  private String content;

  @Column("order_index")
  private int orderIndex;

  @Column("created_at")
  private Instant createdAt;

  @Version private long version = 1;

  /** Creates a new section for a report. */
  public ReportSection(
      UUID organizationId,
      UUID reportId,
      String sectionType,
      String title,
      String content,
      int orderIndex) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(reportId, "reportId");
    Assert.notBlank(sectionType, "sectionType");
    Assert.notNull(content, "content");
    Assert.isTrue(orderIndex >= 0, "orderIndex must not be negative");
    this.organizationId = organizationId;
    this.reportId = reportId;
    this.sectionType = sectionType;
    this.title = title;
    this.content = content;
    this.orderIndex = orderIndex;
    this.createdAt = Instant.now();
  }

  protected ReportSection() {}

  /** Replaces the section payload and ordering. */
  public void update(String title, String content, int orderIndex) {
    Assert.notNull(content, "content");
    Assert.isTrue(orderIndex >= 0, "orderIndex must not be negative");
    this.title = title;
    this.content = content;
    this.orderIndex = orderIndex;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getReportId() {
    return reportId;
  }

  public String getSectionType() {
    return sectionType;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }

  public int getOrderIndex() {
    return orderIndex;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public long getVersion() {
    return version;
  }

  public void setId(UUID id) {
    this.id = id;
  }
}
