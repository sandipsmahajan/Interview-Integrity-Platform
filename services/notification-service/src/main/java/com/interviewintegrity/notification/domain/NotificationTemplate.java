package com.interviewintegrity.notification.domain;

import com.interviewintegrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** A message template. A null organization id provides a platform default. */
@Table("notification_templates")
public class NotificationTemplate implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  private String code;

  private NotificationChannel channel;

  private String subject;

  @Column("body_template")
  private String bodyTemplate;

  private String locale;

  @Column("is_default")
  private boolean defaultFlag;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_at")
  private Instant updatedAt;

  @Column("deleted_at")
  private Instant deletedAt;

  @Version private long version = 1;

  /** Creates a new template for an organization (or platform wide when the id is null). */
  public NotificationTemplate(
      UUID organizationId,
      String code,
      NotificationChannel channel,
      String subject,
      String bodyTemplate,
      String locale) {
    Assert.notBlank(code, "code");
    Assert.notNull(channel, "channel");
    Assert.notBlank(bodyTemplate, "bodyTemplate");
    this.organizationId = organizationId;
    this.code = code;
    this.channel = channel;
    this.subject = subject;
    this.bodyTemplate = bodyTemplate;
    this.locale = locale == null || locale.isBlank() ? "en" : locale;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected NotificationTemplate() {}

  /** Updates the template body and metadata. */
  public void update(String subject, String bodyTemplate, String locale) {
    Assert.notBlank(bodyTemplate, "bodyTemplate");
    this.subject = subject;
    this.bodyTemplate = bodyTemplate;
    if (locale != null && !locale.isBlank()) {
      this.locale = locale;
    }
    this.updatedAt = Instant.now();
  }

  /** Marks the template as the tenant default for its code and channel. */
  public void setDefault(boolean isDefault) {
    this.defaultFlag = isDefault;
    this.updatedAt = Instant.now();
  }

  /** Marks the template as soft deleted. */
  public void delete() {
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

  public String getCode() {
    return code;
  }

  public NotificationChannel getChannel() {
    return channel;
  }

  public String getSubject() {
    return subject;
  }

  public String getBodyTemplate() {
    return bodyTemplate;
  }

  public String getLocale() {
    return locale;
  }

  public boolean isDefault() {
    return defaultFlag;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
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
