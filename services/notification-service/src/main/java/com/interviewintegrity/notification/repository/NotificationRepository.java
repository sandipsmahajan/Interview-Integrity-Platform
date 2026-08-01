package com.interviewintegrity.notification.repository;

import com.interviewintegrity.notification.domain.Notification;
import com.interviewintegrity.notification.domain.NotificationStatus;
import java.util.UUID;
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
}
