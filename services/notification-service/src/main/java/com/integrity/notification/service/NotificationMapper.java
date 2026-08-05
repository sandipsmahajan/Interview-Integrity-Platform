package com.integrity.notification.service;

import com.integrity.notification.domain.Notification;
import com.integrity.notification.domain.NotificationDelivery;
import com.integrity.notification.domain.NotificationPreference;
import com.integrity.notification.domain.NotificationTemplate;
import com.integrity.notification.web.dto.NotificationDeliveryResponse;
import com.integrity.notification.web.dto.NotificationPreferenceResponse;
import com.integrity.notification.web.dto.NotificationResponse;
import com.integrity.notification.web.dto.NotificationTemplateResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps notification-service domain entities into their wire DTO records.
 *
 * <p>Field names on the records match the entity accessors, so MapStruct generates the
 * implementations at compile time.
 */
@Mapper(componentModel = "spring")
public interface NotificationMapper {

  /** Maps a notification into its public response. */
  NotificationResponse toResponse(Notification notification);

  /** Maps a notification delivery into its public response. */
  NotificationDeliveryResponse toDeliveryResponse(NotificationDelivery delivery);

  /** Maps a notification preference into its public response. */
  NotificationPreferenceResponse toResponse(NotificationPreference preference);

  /** Maps a notification template into its public response. */
  @Mapping(target = "isDefault", source = "default")
  NotificationTemplateResponse toResponse(NotificationTemplate template);
}
