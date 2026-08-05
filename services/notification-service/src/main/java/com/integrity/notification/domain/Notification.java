package com.integrity.notification.domain;

import com.integrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** An outbound notification record for a user. */
@Table("notifications")
public class Notification implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("user_id")
  private UUID userId;

  @Column("notification_type")
  private String notificationType;

  private NotificationChannel channel;

  /** Recipient address for email dispatch; null for non-email channels. */
  private String recipient;

  private String subject;

  private String body;

  private NotificationPriority priority;

  private NotificationStatus status;

  @Column("scheduled_at")
  private Instant scheduledAt;

  @Column("sent_at")
  private Instant sentAt;

  @Column("read_at")
  private Instant readAt;

  @Column("created_by")
  private UUID createdBy;

  @Column("source_event_id")
  private UUID sourceEventId;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_at")
  private Instant updatedAt;

  @Version private long version = 1;

  /** Creates a new pending notification. */
  public Notification(
      UUID organizationId,
      UUID userId,
      String notificationType,
      NotificationChannel channel,
      String subject,
      String body,
      NotificationPriority priority,
      Instant scheduledAt,
      UUID createdBy) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(userId, "userId");
    Assert.notBlank(notificationType, "notificationType");
    Assert.notNull(channel, "channel");
    Assert.notBlank(body, "body");
    Assert.notNull(priority, "priority");
    this.organizationId = organizationId;
    this.userId = userId;
    this.notificationType = notificationType;
    this.channel = channel;
    this.subject = subject;
    this.body = body;
    this.priority = priority;
    this.scheduledAt = scheduledAt;
    this.createdBy = createdBy;
    this.status = NotificationStatus.PENDING;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected Notification() {}

  /** Marks the notification as dispatched to the provider. */
  public void markSent() {
    if (this.sentAt == null) {
      this.sentAt = Instant.now();
    }
    this.status = NotificationStatus.SENT;
    this.updatedAt = Instant.now();
  }

  /** Marks the notification as delivered to the recipient. */
  public void markDelivered() {
    if (this.sentAt == null) {
      this.sentAt = Instant.now();
    }
    this.status = NotificationStatus.DELIVERED;
    this.updatedAt = Instant.now();
  }

  /** Marks the notification as failed. */
  public void markFailed() {
    this.status = NotificationStatus.FAILED;
    this.updatedAt = Instant.now();
  }

  /** Marks the notification as read by the recipient. */
  public void markRead() {
    if (this.readAt == null) {
      this.readAt = Instant.now();
    }
    if (this.status == NotificationStatus.DELIVERED || this.status == NotificationStatus.SENT) {
      this.status = NotificationStatus.READ;
    }
    this.updatedAt = Instant.now();
  }

  @Override
  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getNotificationType() {
    return notificationType;
  }

  public NotificationChannel getChannel() {
    return channel;
  }

  public String getRecipient() {
    return recipient;
  }

  /** Sets the recipient address (used for email dispatch). */
  public void setRecipient(String recipient) {
    this.recipient = recipient;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  public NotificationPriority getPriority() {
    return priority;
  }

  public NotificationStatus getStatus() {
    return status;
  }

  public Instant getScheduledAt() {
    return scheduledAt;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public Instant getReadAt() {
    return readAt;
  }

  public UUID getCreatedBy() {
    return createdBy;
  }

  public UUID getSourceEventId() {
    return sourceEventId;
  }

  /** Records the event that produced this notification, for consumer idempotency. */
  public void setSourceEventId(UUID sourceEventId) {
    this.sourceEventId = sourceEventId;
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
