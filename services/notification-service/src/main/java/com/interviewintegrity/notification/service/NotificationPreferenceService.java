package com.interviewintegrity.notification.service;

import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.notification.domain.NotificationChannel;
import com.interviewintegrity.notification.domain.NotificationPreference;
import com.interviewintegrity.notification.repository.NotificationPreferenceRepository;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages per-user notification opt-in/opt-out preferences. */
public class NotificationPreferenceService {

  private final NotificationPreferenceRepository preferenceRepository;

  /** Wires the service with its repository. */
  public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository) {
    this.preferenceRepository = preferenceRepository;
  }

  /** Creates or updates the preference of a user for a channel and notification type. */
  @Transactional
  public Mono<NotificationPreference> setPreference(
      UUID organizationId,
      UUID userId,
      NotificationChannel channel,
      String notificationType,
      boolean enabled) {
    return preferenceRepository
        .findByUserChannelAndType(userId, channel, notificationType)
        .defaultIfEmpty(
            new NotificationPreference(organizationId, userId, channel, notificationType))
        .flatMap(
            preference -> {
              preference.setEnabled(enabled);
              return preferenceRepository.save(preference);
            });
  }

  /** Returns the preference of a user for a channel and notification type. */
  @Transactional(readOnly = true)
  public Mono<NotificationPreference> getPreference(
      UUID userId, NotificationChannel channel, String notificationType) {
    return preferenceRepository
        .findByUserChannelAndType(userId, channel, notificationType)
        .switchIfEmpty(
            Mono.error(
                new NotFoundException(
                    "No preference for channel " + channel + " and type " + notificationType)));
  }

  /** Lists the preferences of a user within the organization. */
  @Transactional(readOnly = true)
  public Flux<NotificationPreference> listPreferences(UUID userId, UUID organizationId) {
    return preferenceRepository.listByUserAndOrganization(userId, organizationId);
  }
}
