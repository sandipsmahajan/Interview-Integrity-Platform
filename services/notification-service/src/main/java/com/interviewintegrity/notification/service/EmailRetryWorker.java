package com.interviewintegrity.notification.service;

import com.interviewintegrity.notification.config.MailProperties;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

/**
 * Periodically scans for pending email notifications and retries their dispatch.
 *
 * <p>The worker runs on the configured interval and lets {@link EmailDispatchService} decide
 * whether a pending notification is due (exponential backoff) or has exhausted its attempts.
 */
public final class EmailRetryWorker {

  private static final Logger log = LoggerFactory.getLogger(EmailRetryWorker.class);

  private final EmailDispatchService dispatchService;
  private final MailProperties mailProperties;

  /** Wires the worker with the dispatch service and retry settings. */
  public EmailRetryWorker(EmailDispatchService dispatchService, MailProperties mailProperties) {
    this.dispatchService = dispatchService;
    this.mailProperties = mailProperties;
  }

  /** Starts the periodic retry loop; the returned disposable stops it. */
  public Disposable start() {
    Duration interval = mailProperties.getWorkerInterval();
    return Flux.interval(interval, interval)
        .flatMap(tick -> dispatchService.dispatchDueOnce(100))
        .onErrorContinue(
            (error, item) -> log.error("Email retry scan failed: {}", error.getMessage(), error))
        .subscribe(
            ignored -> {},
            error -> log.error("Email retry worker terminated: {}", error.getMessage(), error));
  }
}
