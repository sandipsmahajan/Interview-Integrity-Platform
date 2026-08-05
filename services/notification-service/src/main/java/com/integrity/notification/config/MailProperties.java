package com.integrity.notification.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for outbound email delivery.
 *
 * <p>{@code from} is the address used in the From header, {@code maxAttempts} caps the number of
 * dispatch tries before a notification is permanently failed, {@code retryBaseDelay} is the
 * exponential backoff base between attempts and {@code workerInterval} controls how often the retry
 * worker scans for pending emails.
 */
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

  private String from = "no-reply@integritypro.app";
  private int maxAttempts = 3;
  private Duration retryBaseDelay = Duration.ofMinutes(1);
  private Duration workerInterval = Duration.ofSeconds(30);
  private Duration claimLease = Duration.ofMinutes(5);

  /** Returns the From address used for outbound email. */
  public String getFrom() {
    return from;
  }

  /** Sets the From address used for outbound email. */
  public void setFrom(String from) {
    this.from = from;
  }

  /** Returns the maximum dispatch attempts before a notification fails permanently. */
  public int getMaxAttempts() {
    return maxAttempts;
  }

  /** Sets the maximum dispatch attempts before a notification fails permanently. */
  public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  /** Returns the exponential backoff base between dispatch attempts. */
  public Duration getRetryBaseDelay() {
    return retryBaseDelay;
  }

  /** Sets the exponential backoff base between dispatch attempts. */
  public void setRetryBaseDelay(Duration retryBaseDelay) {
    this.retryBaseDelay = retryBaseDelay;
  }

  /** Returns how often the retry worker scans for pending emails. */
  public Duration getWorkerInterval() {
    return workerInterval;
  }

  /** Sets how often the retry worker scans for pending emails. */
  public void setWorkerInterval(Duration workerInterval) {
    this.workerInterval = workerInterval;
  }

  /** Returns how long a dispatch claim stays valid before another worker may retry it. */
  public Duration getClaimLease() {
    return claimLease;
  }

  /** Sets how long a dispatch claim stays valid before another worker may retry it. */
  public void setClaimLease(Duration claimLease) {
    this.claimLease = claimLease;
  }
}
