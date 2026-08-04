package com.interviewintegrity.notification.domain;

import com.interviewintegrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.domain.Persistable;

/** Delivery attempt history of a notification (1 notification : N deliveries). */
@Table("notification_deliveries")
public class NotificationDelivery implements Persistable<Long> {

  @Id private Long id;

  @Column("notification_id")
  private UUID notificationId;

  private NotificationChannel channel;

  private String provider;

  @Column("provider_message_id")
  private String providerMessageId;

  private NotificationStatus status;

  private int attempts;

  @Column("last_error")
  private String lastError;

  @Column("sent_at")
  private Instant sentAt;

  @Column("created_at")
  private Instant createdAt;

  /** Creates a first delivery attempt for a notification. */
  public NotificationDelivery(
      UUID notificationId,
      NotificationChannel channel,
      String provider,
      NotificationStatus status) {
    Assert.notNull(notificationId, "notificationId");
    Assert.notNull(channel, "channel");
    Assert.notBlank(provider, "provider");
    Assert.notNull(status, "status");
    this.notificationId = notificationId;
    this.channel = channel;
    this.provider = provider;
    this.status = status;
    this.attempts = 1;
    if (status == NotificationStatus.SENT || status == NotificationStatus.DELIVERED) {
      this.sentAt = Instant.now();
    }
    this.createdAt = Instant.now();
  }

  protected NotificationDelivery() {}

  /** Attaches the provider message id returned on a successful dispatch. */
  public void attachProviderMessageId(String providerMessageId) {
    this.providerMessageId = providerMessageId;
  }

  /** Records the failure detail of a delivery attempt. */
  public void noteError(String error) {
    if (error != null) {
      this.lastError = error;
    }
  }

  @Override
  public Long getId() {
    return id;
  }

  public UUID getNotificationId() {
    return notificationId;
  }

  public NotificationChannel getChannel() {
    return channel;
  }

  public String getProvider() {
    return provider;
  }

  public String getProviderMessageId() {
    return providerMessageId;
  }

  public NotificationStatus getStatus() {
    return status;
  }

  public int getAttempts() {
    return attempts;
  }

  public String getLastError() {
    return lastError;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
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
