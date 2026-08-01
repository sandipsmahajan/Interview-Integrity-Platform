package com.interviewintegrity.notification.repository;

import com.interviewintegrity.notification.domain.NotificationChannel;
import com.interviewintegrity.notification.domain.NotificationTemplate;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link NotificationTemplate} entities. */
public interface NotificationTemplateRepository
    extends ReactiveCrudRepository<NotificationTemplate, UUID> {

  /** Finds a live template by id. */
  @Query("SELECT * FROM notification_templates WHERE id = :id AND deleted_at IS NULL")
  Mono<NotificationTemplate> findLiveById(UUID id);

  /** Finds the live tenant template matching code, channel and locale. */
  @Query(
      "SELECT * FROM notification_templates WHERE organization_id = :organizationId "
          + "AND code = :code AND channel = :channel AND locale = :locale AND deleted_at IS NULL")
  Mono<NotificationTemplate> findLiveByOrganizationCodeChannelLocale(
      UUID organizationId, String code, NotificationChannel channel, String locale);

  /** Finds the live platform default template matching code, channel and locale. */
  @Query(
      "SELECT * FROM notification_templates WHERE organization_id IS NULL "
          + "AND code = :code AND channel = :channel AND locale = :locale AND deleted_at IS NULL")
  Mono<NotificationTemplate> findLivePlatformDefault(
      String code, NotificationChannel channel, String locale);

  /** Lists the live templates of an organization, code ordered. */
  @Query(
      "SELECT * FROM notification_templates WHERE organization_id = :organizationId "
          + "AND deleted_at IS NULL ORDER BY code")
  Flux<NotificationTemplate> listLiveByOrganization(UUID organizationId);

  /** Lists the live platform default templates. */
  @Query(
      "SELECT * FROM notification_templates WHERE organization_id IS NULL "
          + "AND deleted_at IS NULL ORDER BY code")
  Flux<NotificationTemplate> listLivePlatformDefaults();
}
