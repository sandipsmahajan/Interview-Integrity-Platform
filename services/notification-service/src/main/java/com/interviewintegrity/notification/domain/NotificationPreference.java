package com.interviewintegrity.notification.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Per-user opt-in/opt-out for a notification channel and type. */
@Table("notification_preferences")
public class NotificationPreference {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("user_id")
  private UUID userId;

  private NotificationChannel channel;

  @Column("notification_type")
  private String notificationType;

  private boolean enabled;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_at")
  private Instant updatedAt;

  @Version private long version = 1;

  /** Creates a preference that is enabled by default. */
  public NotificationPreference(
      UUID organizationId, UUID userId, NotificationChannel channel, String notificationType) {
    this.organizationId = organizationId;
    this.userId = userId;
    this.channel = channel;
    this.notificationType = notificationType;
    this.enabled = true;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected NotificationPreference() {}

  /** Opts the user in or out of the channel and type. */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    this.updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getUserId() {
    return userId;
  }

  public NotificationChannel getChannel() {
    return channel;
  }

  public String getNotificationType() {
    return notificationType;
  }

  public boolean isEnabled() {
    return enabled;
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
}
