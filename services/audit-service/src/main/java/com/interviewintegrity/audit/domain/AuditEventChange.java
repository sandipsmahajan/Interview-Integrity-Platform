package com.interviewintegrity.audit.domain;

import com.interviewintegrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.domain.Persistable;

/** Field-level change detail recorded alongside an audit event. */
@Table("audit_event_changes")
public class AuditEventChange implements Persistable<Long> {

  @Id private Long id;

  @Column("audit_event_id")
  private UUID auditEventId;

  @Column("occurred_at")
  private Instant occurredAt;

  private String field;

  @Column("old_value")
  private String oldValue;

  @Column("new_value")
  private String newValue;

  /** Creates a change record for an audit event. */
  public AuditEventChange(
      UUID auditEventId, Instant occurredAt, String field, String oldValue, String newValue) {
    Assert.notNull(auditEventId, "auditEventId");
    Assert.notBlank(field, "field");
    this.auditEventId = auditEventId;
    this.occurredAt = occurredAt;
    this.field = field;
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  protected AuditEventChange() {}

  @Override
  public Long getId() {
    return id;
  }

  public UUID getAuditEventId() {
    return auditEventId;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public String getField() {
    return field;
  }

  public String getOldValue() {
    return oldValue;
  }

  public String getNewValue() {
    return newValue;
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
