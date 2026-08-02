package com.interviewintegrity.notification.repository;

import com.interviewintegrity.notification.domain.Notification;
import com.interviewintegrity.notification.domain.NotificationStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link Notification} entities. */
public interface NotificationRepository extends ReactiveCrudRepository<Notification, UUID> {

  /** Finds a notification by id within an organization. */
  @Query("SELECT * FROM notifications WHERE id = :id AND organization_id = :organizationId")
  Mono<Notification> findByIdAndOrganization(UUID id, UUID organizationId);

  /** Lists the notifications of a user within an organization, newest first. */
  @Query(
      "SELECT * FROM notifications WHERE user_id = :userId "
          + "AND organization_id = :organizationId ORDER BY created_at DESC")
  Flux<Notification> listByUserAndOrganization(UUID userId, UUID organizationId);

  /** Lists the pending notifications of an organization, scheduled order first. */
  @Query(
      "SELECT * FROM notifications WHERE organization_id = :organizationId "
          + "AND status = :status ORDER BY created_at")
  Flux<Notification> listByOrganizationAndStatus(UUID organizationId, NotificationStatus status);

  /** Lists the pending email notifications that are ready for (re)dispatch. */
  @Query(
      "SELECT * FROM notifications WHERE channel = 'EMAIL' AND status = 'PENDING' "
          + "AND (scheduled_at IS NULL OR scheduled_at <= :now) "
          + "ORDER BY created_at LIMIT :limit")
  Flux<Notification> listPendingEmailDue(Instant now, int limit);

  /**
   * Atomically claims a pending notification for dispatch, returning the number of rows affected.
   *
   * <p>The guarded WHERE clause prevents two workers (or the consumer racing the retry worker) from
   * dispatching the same notification twice: only an unclaimed or lease-expired notification in
   * {@code PENDING} state is claimed.
   */
  @Modifying
  @Query(
      "UPDATE notifications SET claimed_at = :claimedAt "
          + "WHERE id = :id AND status = 'PENDING' "
          + "AND (claimed_at IS NULL OR claimed_at < :leaseExpiry)")
  Mono<Integer> claimForDispatch(UUID id, Instant claimedAt, Instant leaseExpiry);

  /** Releases a dispatch claim so the notification can be retried by another worker. */
  @Modifying
  @Query("UPDATE notifications SET claimed_at = NULL WHERE id = :id")
  Mono<Integer> releaseClaim(UUID id);
}
