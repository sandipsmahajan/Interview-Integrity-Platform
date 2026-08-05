package com.integrity.notification.repository;

import com.integrity.notification.domain.NotificationDelivery;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link NotificationDelivery} entities. */
public interface NotificationDeliveryRepository
    extends ReactiveCrudRepository<NotificationDelivery, Long> {

  /** Lists the delivery attempts of a notification, newest first. */
  @Query(
      "SELECT * FROM notification_deliveries WHERE notification_id = :notificationId "
          + "ORDER BY created_at DESC")
  Flux<NotificationDelivery> listByNotification(UUID notificationId);

  /** Returns the number of delivery attempts of a notification. */
  @Query("SELECT count(*) FROM notification_deliveries WHERE notification_id = :notificationId")
  Mono<Long> countByNotification(UUID notificationId);
}
