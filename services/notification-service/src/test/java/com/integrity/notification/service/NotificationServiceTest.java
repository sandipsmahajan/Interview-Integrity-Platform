package com.integrity.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.integrity.exception.NotFoundException;
import com.integrity.notification.domain.Notification;
import com.integrity.notification.domain.NotificationChannel;
import com.integrity.notification.domain.NotificationDelivery;
import com.integrity.notification.domain.NotificationPriority;
import com.integrity.notification.domain.NotificationStatus;
import com.integrity.notification.repository.NotificationDeliveryRepository;
import com.integrity.notification.repository.NotificationRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the notification service. */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock private NotificationRepository notificationRepository;
  @Mock private NotificationDeliveryRepository deliveryRepository;

  private NotificationService notificationService;

  @BeforeEach
  void setUp() {
    notificationService = new NotificationService(notificationRepository, deliveryRepository);
  }

  @Test
  void createNotificationSavesAsPending() {
    UUID organizationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID createdBy = UUID.randomUUID();
    when(notificationRepository.save(any(Notification.class)))
        .thenAnswer(
            invocation -> {
              Notification notification = invocation.getArgument(0);
              notification.setId(UUID.randomUUID());
              return Mono.just(notification);
            });

    StepVerifier.create(
            notificationService.createNotification(
                organizationId,
                userId,
                "interview_reminder",
                NotificationChannel.EMAIL,
                "Interview reminder",
                "Your interview is tomorrow.",
                NotificationPriority.HIGH,
                null,
                createdBy))
        .assertNext(
            notification -> {
              org.assertj.core.api.Assertions.assertThat(notification.getStatus())
                  .isEqualTo(NotificationStatus.PENDING);
              org.assertj.core.api.Assertions.assertThat(notification.getOrganizationId())
                  .isEqualTo(organizationId);
              org.assertj.core.api.Assertions.assertThat(notification.getChannel())
                  .isEqualTo(NotificationChannel.EMAIL);
            })
        .verifyComplete();
  }

  @Test
  void createEmailNotificationFromEventSkipsDuplicateEvent() {
    UUID organizationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID sourceEventId = UUID.randomUUID();
    Notification existing = newNotification(organizationId);
    when(notificationRepository.findBySourceEventId(sourceEventId)).thenReturn(Mono.just(existing));

    StepVerifier.create(
            notificationService.createEmailNotificationFromEvent(
                organizationId,
                userId,
                "candidate@example.com",
                "interview_reminder",
                "Interview reminder",
                "Your interview is tomorrow.",
                NotificationPriority.HIGH,
                sourceEventId))
        .expectNext(existing)
        .verifyComplete();

    verify(notificationRepository, never()).save(any(Notification.class));
  }

  @Test
  void createEmailNotificationFromEventStampsSourceEvent() {
    UUID organizationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID sourceEventId = UUID.randomUUID();
    when(notificationRepository.findBySourceEventId(sourceEventId)).thenReturn(Mono.empty());
    when(notificationRepository.save(any(Notification.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            notificationService.createEmailNotificationFromEvent(
                organizationId,
                userId,
                "candidate@example.com",
                "interview_reminder",
                "Interview reminder",
                "Your interview is tomorrow.",
                NotificationPriority.HIGH,
                sourceEventId))
        .assertNext(
            created -> {
              org.assertj.core.api.Assertions.assertThat(created.getSourceEventId())
                  .isEqualTo(sourceEventId);
              org.assertj.core.api.Assertions.assertThat(created.getRecipient())
                  .isEqualTo("candidate@example.com");
            })
        .verifyComplete();
  }

  @Test
  void markSentMarksNotificationAndRecordsDelivery() {
    UUID organizationId = UUID.randomUUID();
    Notification notification = newNotification(organizationId);
    when(notificationRepository.findByIdAndOrganization(notification.getId(), organizationId))
        .thenReturn(Mono.just(notification));
    when(notificationRepository.save(any(Notification.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(deliveryRepository.save(any(NotificationDelivery.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            notificationService.markSent(
                notification.getId(), organizationId, "ses", "provider-msg-1"))
        .assertNext(
            sent -> {
              org.assertj.core.api.Assertions.assertThat(sent.getStatus())
                  .isEqualTo(NotificationStatus.SENT);
              org.assertj.core.api.Assertions.assertThat(sent.getSentAt()).isNotNull();
            })
        .verifyComplete();

    verify(deliveryRepository).save(any(NotificationDelivery.class));
  }

  @Test
  void markFailedRecordsDeliveryWithError() {
    UUID organizationId = UUID.randomUUID();
    Notification notification = newNotification(organizationId);
    when(notificationRepository.findByIdAndOrganization(notification.getId(), organizationId))
        .thenReturn(Mono.just(notification));
    when(notificationRepository.save(any(Notification.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(deliveryRepository.save(any(NotificationDelivery.class)))
        .thenAnswer(
            invocation -> {
              NotificationDelivery delivery = invocation.getArgument(0);
              delivery.setId(1L);
              return Mono.just(delivery);
            });

    StepVerifier.create(
            notificationService.markFailed(notification.getId(), organizationId, "ses", "bounce"))
        .assertNext(
            failed -> {
              org.assertj.core.api.Assertions.assertThat(failed.getStatus())
                  .isEqualTo(NotificationStatus.FAILED);
            })
        .verifyComplete();

    verify(deliveryRepository)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                delivery -> "bounce".equals(delivery.getLastError())));
  }

  @Test
  void markReadFailsForUnknownNotification() {
    UUID notificationId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(notificationRepository.findByIdAndOrganizationAndUser(
            notificationId, organizationId, userId))
        .thenReturn(Mono.empty());

    StepVerifier.create(notificationService.markRead(notificationId, organizationId, userId))
        .expectError(NotFoundException.class)
        .verify();

    verify(notificationRepository, never()).save(any());
  }

  private static Notification newNotification(UUID organizationId) {
    Notification notification =
        new Notification(
            organizationId,
            UUID.randomUUID(),
            "interview_reminder",
            NotificationChannel.EMAIL,
            "Interview reminder",
            "Your interview is tomorrow.",
            NotificationPriority.HIGH,
            null,
            UUID.randomUUID());
    notification.setId(UUID.randomUUID());
    return notification;
  }
}
