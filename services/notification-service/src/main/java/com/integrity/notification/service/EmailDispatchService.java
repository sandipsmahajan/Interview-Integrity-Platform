package com.integrity.notification.service;

import com.integrity.notification.config.MailProperties;
import com.integrity.notification.domain.Notification;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Coordinates email dispatch attempts and the retry lifecycle.
 *
 * <p>A successful send marks the notification as {@code SENT} and records a delivery. A failed send
 * records a failed delivery and, once the maximum attempt count is reached, marks the notification
 * as permanently {@code FAILED}; otherwise the notification stays pending for the retry worker
 * which applies an exponential backoff between attempts.
 */
public final class EmailDispatchService {

  private static final Logger log = LoggerFactory.getLogger(EmailDispatchService.class);
  private static final String PROVIDER = "smtp";

  private final NotificationService notificationService;
  private final EmailDispatcher emailDispatcher;
  private final MailProperties mailProperties;

  /** Wires the dispatch service with its collaborators. */
  public EmailDispatchService(
      NotificationService notificationService,
      EmailDispatcher emailDispatcher,
      MailProperties mailProperties) {
    this.notificationService = notificationService;
    this.emailDispatcher = emailDispatcher;
    this.mailProperties = mailProperties;
  }

  /** Dispatches a freshly created notification, recording the attempt outcome. */
  public Mono<Void> dispatch(Notification notification) {
    return claimThenRelease(notification, () -> dispatchAttempt(notification).then());
  }

  /** Scans for pending emails that are due for dispatch and retries them. */
  public Mono<Void> dispatchDueOnce(int limit) {
    Instant now = Instant.now();
    return notificationService
        .listPendingEmailDue(now, limit)
        .flatMap(
            notification ->
                isDue(notification, now)
                    .flatMap(due -> due ? Mono.just(notification) : Mono.empty()))
        .concatMap(this::dispatch)
        .then();
  }

  /**
   * Claims the notification atomically, runs the given dispatch action only when the claim was won
   * and releases the claim afterwards so a crashed worker's lease does not block retries forever.
   *
   * <p>The claim is only released by the worker that won it; a worker that lost the race must not
   * touch the lease, otherwise it would clear the winner's claim and let a third worker dispatch
   * the same notification a second time.
   */
  private Mono<Void> claimThenRelease(
      Notification notification, java.util.function.Supplier<Mono<Void>> dispatchAction) {
    Instant now = Instant.now();
    Instant leaseExpiry = now.minus(mailProperties.getClaimLease());
    return notificationService
        .claimForDispatch(notification.getId(), now, leaseExpiry)
        .flatMap(
            claimed -> {
              if (!claimed) {
                return Mono.empty();
              }
              return dispatchAction
                  .get()
                  .thenReturn(notification.getId())
                  .onErrorResume(
                      error ->
                          notificationService
                              .releaseClaim(notification.getId())
                              .then(Mono.error(error)));
            })
        .flatMap(notificationId -> notificationService.releaseClaim(notificationId))
        .then();
  }

  private Mono<Boolean> isDue(Notification notification, Instant now) {
    return notificationService
        .countAttempts(notification.getId())
        .flatMap(
            attempts -> {
              if (attempts == 0) {
                return Mono.just(true);
              }
              if (attempts >= mailProperties.getMaxAttempts()) {
                return notificationService
                    .markFailed(
                        notification.getId(),
                        notification.getOrganizationId(),
                        PROVIDER,
                        "Max attempts reached")
                    .thenReturn(false);
              }
              Duration backoff =
                  mailProperties.getRetryBaseDelay().multipliedBy(1L << (attempts - 1));
              return notificationService
                  .latestAttemptAt(notification.getId())
                  .map(lastAttempt -> now.isAfter(lastAttempt.plus(backoff)))
                  .defaultIfEmpty(true);
            });
  }

  private Mono<Notification> dispatchAttempt(Notification notification) {
    return emailDispatcher
        .send(
            notification.getRecipient(),
            notification.getSubject(),
            notification.getBody(),
            EmailTemplateEngine.toPlainText(notification.getBody()))
        .flatMap(
            messageId ->
                notificationService.markSent(
                    notification.getId(), notification.getOrganizationId(), PROVIDER, messageId))
        .onErrorResume(error -> handleFailure(notification, error.getMessage()));
  }

  private Mono<Notification> handleFailure(Notification notification, String errorMessage) {
    if (log.isWarnEnabled()) {
      log.warn(
          "Email dispatch failed for notification {} to {}: {}",
          notification.getId(),
          notification.getRecipient(),
          errorMessage);
    }
    return notificationService
        .countAttempts(notification.getId())
        .flatMap(
            attempts ->
                attempts >= mailProperties.getMaxAttempts()
                    ? notificationService.markFailed(
                        notification.getId(),
                        notification.getOrganizationId(),
                        PROVIDER,
                        errorMessage)
                    : notificationService
                        .recordFailedAttempt(
                            notification.getId(),
                            notification.getOrganizationId(),
                            PROVIDER,
                            errorMessage)
                        .thenReturn(notification));
  }
}
