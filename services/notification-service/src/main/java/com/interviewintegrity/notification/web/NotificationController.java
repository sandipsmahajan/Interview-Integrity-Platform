package com.interviewintegrity.notification.web;

import com.interviewintegrity.notification.domain.Notification;
import com.interviewintegrity.notification.service.NotificationService;
import com.interviewintegrity.notification.web.dto.CreateNotificationRequest;
import com.interviewintegrity.notification.web.dto.DeliveryOutcomeRequest;
import com.interviewintegrity.notification.web.dto.NotificationDeliveryResponse;
import com.interviewintegrity.notification.web.dto.NotificationResponse;
import com.interviewintegrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Notification endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Manage outbound notifications")
public final class NotificationController {

  private final NotificationService notificationService;

  /** Creates the controller bound to the notification service. */
  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  /** Creates a notification for a user. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a notification")
  public Mono<NotificationResponse> create(
      Authentication authentication, @Valid @RequestBody CreateNotificationRequest request) {
    return notificationService
        .createNotification(
            SecurityPrincipals.organizationId(authentication),
            request.userId(),
            request.notificationType().trim(),
            request.channel(),
            request.subject(),
            request.body(),
            request.priority(),
            request.scheduledAt(),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  /** Lists the notifications of a user, optionally filtered by status. */
  @GetMapping
  @Operation(summary = "List notifications")
  public Flux<NotificationResponse> list(
      Authentication authentication,
      @RequestParam UUID userId,
      @RequestParam(required = false) String status) {
    return notificationService
        .listByUser(userId, SecurityPrincipals.organizationId(authentication))
        .filter(
            notification ->
                status == null || notification.getStatus().name().equalsIgnoreCase(status))
        .map(this::toResponse);
  }

  /** Returns a single notification. */
  @GetMapping("/{notificationId}")
  @Operation(summary = "Get a notification")
  public Mono<NotificationResponse> get(
      Authentication authentication, @PathVariable UUID notificationId) {
    return notificationService
        .getNotification(notificationId, SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  /** Marks a notification as dispatched and records the delivery attempt. */
  @PostMapping("/{notificationId}/sent")
  @Operation(summary = "Mark a notification as sent")
  public Mono<NotificationResponse> markSent(
      Authentication authentication,
      @PathVariable UUID notificationId,
      @Valid @RequestBody DeliveryOutcomeRequest request) {
    return notificationService
        .markSent(
            notificationId,
            SecurityPrincipals.organizationId(authentication),
            request.provider(),
            request.providerMessageId())
        .map(this::toResponse);
  }

  /** Marks a notification as delivered and records the delivery attempt. */
  @PostMapping("/{notificationId}/delivered")
  @Operation(summary = "Mark a notification as delivered")
  public Mono<NotificationResponse> markDelivered(
      Authentication authentication,
      @PathVariable UUID notificationId,
      @Valid @RequestBody DeliveryOutcomeRequest request) {
    return notificationService
        .markDelivered(
            notificationId,
            SecurityPrincipals.organizationId(authentication),
            request.provider(),
            request.providerMessageId())
        .map(this::toResponse);
  }

  /** Marks a notification as failed and records the failed delivery attempt. */
  @PostMapping("/{notificationId}/failed")
  @Operation(summary = "Mark a notification as failed")
  public Mono<NotificationResponse> markFailed(
      Authentication authentication,
      @PathVariable UUID notificationId,
      @Valid @RequestBody DeliveryOutcomeRequest request) {
    return notificationService
        .markFailed(
            notificationId,
            SecurityPrincipals.organizationId(authentication),
            request.provider(),
            request.errorMessage())
        .map(this::toResponse);
  }

  /** Marks a notification as read by the recipient. */
  @PostMapping("/{notificationId}/read")
  @Operation(summary = "Mark a notification as read")
  public Mono<NotificationResponse> markRead(
      Authentication authentication, @PathVariable UUID notificationId) {
    return notificationService
        .markRead(notificationId, SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  /** Lists the delivery attempts of a notification. */
  @GetMapping("/{notificationId}/deliveries")
  @Operation(summary = "List delivery attempts")
  public Flux<NotificationDeliveryResponse> deliveries(
      Authentication authentication, @PathVariable UUID notificationId) {
    return notificationService
        .listDeliveries(notificationId, SecurityPrincipals.organizationId(authentication))
        .map(this::toDeliveryResponse);
  }

  private NotificationResponse toResponse(Notification notification) {
    return new NotificationResponse(
        notification.getId(),
        notification.getOrganizationId(),
        notification.getUserId(),
        notification.getNotificationType(),
        notification.getChannel(),
        notification.getSubject(),
        notification.getBody(),
        notification.getPriority(),
        notification.getStatus(),
        notification.getScheduledAt(),
        notification.getSentAt(),
        notification.getReadAt(),
        notification.getCreatedAt(),
        notification.getUpdatedAt());
  }

  private NotificationDeliveryResponse toDeliveryResponse(
      com.interviewintegrity.notification.domain.NotificationDelivery delivery) {
    return new NotificationDeliveryResponse(
        delivery.getId(),
        delivery.getNotificationId(),
        delivery.getChannel(),
        delivery.getProvider(),
        delivery.getProviderMessageId(),
        delivery.getStatus(),
        delivery.getAttempts(),
        delivery.getLastError(),
        delivery.getSentAt(),
        delivery.getCreatedAt());
  }
}
