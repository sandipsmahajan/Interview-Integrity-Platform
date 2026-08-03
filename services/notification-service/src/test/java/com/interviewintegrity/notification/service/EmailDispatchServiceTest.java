package com.interviewintegrity.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interviewintegrity.notification.config.MailProperties;
import com.interviewintegrity.notification.domain.Notification;
import com.interviewintegrity.notification.domain.NotificationChannel;
import com.interviewintegrity.notification.domain.NotificationPriority;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the email dispatch claim logic. */
@ExtendWith(MockitoExtension.class)
class EmailDispatchServiceTest {

  @Mock private NotificationService notificationService;
  @Mock private EmailDispatcher emailDispatcher;

  private EmailDispatchService dispatchService;
  private MailProperties mailProperties;

  @BeforeEach
  void setUp() {
    mailProperties = new MailProperties();
    mailProperties.setClaimLease(Duration.ofMinutes(5));
    dispatchService =
        new EmailDispatchService(notificationService, emailDispatcher, mailProperties);
  }

  @Test
  void dispatchSkipsWhenClaimLost() {
    Notification notification = newNotification();
    when(notificationService.claimForDispatch(
            eq(notification.getId()), any(Instant.class), any(Instant.class)))
        .thenReturn(Mono.just(false));

    StepVerifier.create(dispatchService.dispatch(notification)).verifyComplete();

    verify(emailDispatcher, never()).send(any(), any(), any(), any());
    verify(notificationService, never()).releaseClaim(notification.getId());
  }

  @Test
  void dispatchSendsWhenClaimWon() {
    Notification notification = newNotification();
    when(notificationService.claimForDispatch(
            eq(notification.getId()), any(Instant.class), any(Instant.class)))
        .thenReturn(Mono.just(true));
    when(notificationService.releaseClaim(notification.getId())).thenReturn(Mono.empty());
    when(emailDispatcher.send(
            eq(notification.getRecipient()),
            eq(notification.getSubject()),
            eq(notification.getBody()),
            any(String.class)))
        .thenReturn(Mono.just("provider-id"));
    when(notificationService.markSent(
            notification.getId(), notification.getOrganizationId(), "smtp", "provider-id"))
        .thenReturn(Mono.just(notification));

    StepVerifier.create(dispatchService.dispatch(notification)).verifyComplete();

    verify(emailDispatcher)
        .send(
            eq(notification.getRecipient()),
            eq(notification.getSubject()),
            eq(notification.getBody()),
            any(String.class));
    verify(notificationService)
        .markSent(notification.getId(), notification.getOrganizationId(), "smtp", "provider-id");
  }

  private static Notification newNotification() {
    Notification notification =
        new Notification(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "interview_reminder",
            NotificationChannel.EMAIL,
            "Interview reminder",
            "Your interview is tomorrow.",
            NotificationPriority.HIGH,
            null,
            UUID.randomUUID());
    notification.setId(UUID.randomUUID());
    notification.setRecipient("candidate@example.com");
    return notification;
  }
}
