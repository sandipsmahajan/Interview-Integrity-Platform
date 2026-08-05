package com.integrity.notification.service;

import com.integrity.exception.NotFoundException;
import com.integrity.notification.domain.Notification;
import com.integrity.notification.domain.NotificationChannel;
import com.integrity.notification.domain.NotificationDelivery;
import com.integrity.notification.domain.NotificationPriority;
import com.integrity.notification.domain.NotificationStatus;
import com.integrity.notification.repository.NotificationDeliveryRepository;
import com.integrity.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages notifications and their delivery lifecycle. */
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final NotificationDeliveryRepository deliveryRepository;

  /** Wires the service with its repositories. */
  public NotificationService(
      NotificationRepository notificationRepository,
      NotificationDeliveryRepository deliveryRepository) {
    this.notificationRepository = notificationRepository;
    this.deliveryRepository = deliveryRepository;
  }

  /** Creates a pending notification for a user. */
  @Transactional
  public Mono<Notification> createNotification(
      UUID organizationId,
      UUID userId,
      String notificationType,
      NotificationChannel channel,
      String subject,
      String body,
      NotificationPriority priority,
      Instant scheduledAt,
      UUID createdBy) {
    return notificationRepository.save(
        new Notification(
            organizationId,
            userId,
            notificationType,
            channel,
            subject,
            body,
            priority,
            scheduledAt,
            createdBy));
  }

  /** Creates a pending email notification carrying the recipient address. */
  @Transactional
  public Mono<Notification> createEmailNotification(
      UUID organizationId,
      UUID userId,
      String recipient,
      String notificationType,
      String subject,
      String body,
      NotificationPriority priority,
      UUID createdBy) {
    Notification notification =
        new Notification(
            organizationId,
            userId,
            notificationType,
            NotificationChannel.EMAIL,
            subject,
            body,
            priority,
            Instant.now(),
            createdBy);
    notification.setRecipient(recipient);
    return notificationRepository.save(notification);
  }

  /**
   * Creates a pending email notification for a consumed event, skipping when the event was already
   * processed so a Kafka redelivery never sends a duplicate email.
   */
  @Transactional
  public Mono<Notification> createEmailNotificationFromEvent(
      UUID organizationId,
      UUID userId,
      String recipient,
      String notificationType,
      String subject,
      String body,
      NotificationPriority priority,
      UUID sourceEventId) {
    return notificationRepository
        .findBySourceEventId(sourceEventId)
        .switchIfEmpty(
            Mono.defer(
                () ->
                    createEmailNotification(
                            organizationId,
                            userId,
                            recipient,
                            notificationType,
                            subject,
                            body,
                            priority,
                            null)
                        .flatMap(
                            notification -> {
                              notification.setSourceEventId(sourceEventId);
                              return notificationRepository.save(notification);
                            })));
  }

  /** Returns the pending email notifications that are ready for dispatch. */
  @Transactional(readOnly = true)
  public Flux<Notification> listPendingEmailDue(Instant now, int limit) {
    return notificationRepository.listPendingEmailDue(now, limit);
  }

  /**
   * Atomically claims a pending notification for dispatch, returning true when this worker won the
   * claim.
   */
  @Transactional
  public Mono<Boolean> claimForDispatch(
      UUID notificationId, Instant claimedAt, Instant leaseExpiry) {
    return notificationRepository
        .claimForDispatch(notificationId, claimedAt, leaseExpiry)
        .map(affected -> affected > 0)
        .defaultIfEmpty(false);
  }

  /** Releases a dispatch claim so the notification can be retried by another worker. */
  @Transactional
  public Mono<Void> releaseClaim(UUID notificationId) {
    return notificationRepository.releaseClaim(notificationId).then();
  }

  /** Records a failed dispatch attempt while leaving the notification pending for retry. */
  @Transactional
  public Mono<NotificationDelivery> recordFailedAttempt(
      UUID notificationId, UUID organizationId, String provider, String errorMessage) {
    return getNotification(notificationId, organizationId)
        .flatMap(
            notification ->
                recordDelivery(
                    notification, NotificationStatus.FAILED, provider, null, errorMessage));
  }

  /** Returns the number of dispatch attempts of a notification. */
  @Transactional(readOnly = true)
  public Mono<Long> countAttempts(UUID notificationId) {
    return deliveryRepository.countByNotification(notificationId);
  }

  /** Returns the timestamp of the most recent dispatch attempt, if any. */
  @Transactional(readOnly = true)
  public Mono<Instant> latestAttemptAt(UUID notificationId) {
    return deliveryRepository
        .listByNotification(notificationId)
        .next()
        .map(NotificationDelivery::getCreatedAt);
  }

  /** Returns a single notification within the organization. */
  @Transactional(readOnly = true)
  public Mono<Notification> getNotification(UUID notificationId, UUID organizationId) {
    return notificationRepository
        .findByIdAndOrganization(notificationId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Notification not found")));
  }

  /**
   * Returns a single notification owned by a user within the organization.
   *
   * <p>Used by user-facing read and mark-read flows so callers can only access their own
   * notifications, preventing cross-user notification access within a tenant.
   */
  @Transactional(readOnly = true)
  public Mono<Notification> getOwnedNotification(
      UUID notificationId, UUID organizationId, UUID userId) {
    return notificationRepository
        .findByIdAndOrganizationAndUser(notificationId, organizationId, userId)
        .switchIfEmpty(Mono.error(new NotFoundException("Notification not found")));
  }

  /** Lists the notifications of a user within the organization. */
  @Transactional(readOnly = true)
  public Flux<Notification> listByUser(UUID userId, UUID organizationId) {
    return notificationRepository.listByUserAndOrganization(userId, organizationId);
  }

  /** Lists the pending notifications of the organization for dispatch. */
  @Transactional(readOnly = true)
  public Flux<Notification> listPending(UUID organizationId) {
    return notificationRepository.listByOrganizationAndStatus(
        organizationId, NotificationStatus.PENDING);
  }

  /** Marks a notification as dispatched and records the delivery attempt. */
  @Transactional
  public Mono<Notification> markSent(
      UUID notificationId, UUID organizationId, String provider, String providerMessageId) {
    return getNotification(notificationId, organizationId)
        .flatMap(
            notification -> {
              notification.markSent();
              return notificationRepository
                  .save(notification)
                  .flatMap(
                      saved ->
                          recordDelivery(
                                  saved, NotificationStatus.SENT, provider, providerMessageId, null)
                              .thenReturn(saved));
            });
  }

  /** Marks a notification as delivered and records the delivery attempt. */
  @Transactional
  public Mono<Notification> markDelivered(
      UUID notificationId, UUID organizationId, String provider, String providerMessageId) {
    return getNotification(notificationId, organizationId)
        .flatMap(
            notification -> {
              notification.markDelivered();
              return notificationRepository
                  .save(notification)
                  .flatMap(
                      saved ->
                          recordDelivery(
                                  saved,
                                  NotificationStatus.DELIVERED,
                                  provider,
                                  providerMessageId,
                                  null)
                              .thenReturn(saved));
            });
  }

  /** Marks a notification as failed and records the failed delivery attempt. */
  @Transactional
  public Mono<Notification> markFailed(
      UUID notificationId, UUID organizationId, String provider, String errorMessage) {
    return getNotification(notificationId, organizationId)
        .flatMap(
            notification -> {
              notification.markFailed();
              return notificationRepository
                  .save(notification)
                  .flatMap(
                      saved ->
                          recordDelivery(
                                  saved, NotificationStatus.FAILED, provider, null, errorMessage)
                              .thenReturn(saved));
            });
  }

  /** Marks a notification as read by the recipient. */
  @Transactional
  public Mono<Notification> markRead(UUID notificationId, UUID organizationId, UUID userId) {
    return getOwnedNotification(notificationId, organizationId, userId)
        .map(
            notification -> {
              notification.markRead();
              return notification;
            })
        .flatMap(notificationRepository::save);
  }

  /**
   * Lists the delivery attempts of a notification.
   *
   * <p>Callers must first resolve ownership via {@link #getOwnedNotification(UUID, UUID, UUID)} so
   * a user cannot view the delivery history of another user's notification.
   */
  @Transactional(readOnly = true)
  public Flux<NotificationDelivery> listDeliveries(UUID notificationId) {
    return deliveryRepository.listByNotification(notificationId);
  }

  private Mono<NotificationDelivery> recordDelivery(
      Notification notification,
      NotificationStatus status,
      String provider,
      String providerMessageId,
      String errorMessage) {
    NotificationDelivery delivery =
        new NotificationDelivery(notification.getId(), notification.getChannel(), provider, status);
    delivery.attachProviderMessageId(providerMessageId);
    delivery.noteError(errorMessage);
    return deliveryRepository.save(delivery);
  }
}
