package com.interviewintegrity.notification.repository;

import com.interviewintegrity.notification.domain.NotificationChannel;
import com.interviewintegrity.notification.domain.NotificationPreference;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link NotificationPreference} entities. */
public interface NotificationPreferenceRepository
    extends ReactiveCrudRepository<NotificationPreference, UUID> {

  /** Finds the preference of a user for a channel and notification type. */
  @Query(
      "SELECT * FROM notification_preferences WHERE user_id = :userId "
          + "AND channel = :channel AND notification_type = :notificationType")
  Mono<NotificationPreference> findByUserChannelAndType(
      UUID userId, NotificationChannel channel, String notificationType);

  /** Lists the preferences of a user within an organization. */
  @Query(
      "SELECT * FROM notification_preferences WHERE user_id = :userId "
          + "AND organization_id = :organizationId ORDER BY channel, notification_type")
  Flux<NotificationPreference> listByUserAndOrganization(UUID userId, UUID organizationId);
}
