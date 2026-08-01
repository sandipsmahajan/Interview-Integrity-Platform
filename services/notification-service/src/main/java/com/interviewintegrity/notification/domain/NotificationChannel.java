package com.interviewintegrity.notification.domain;

/** Delivery channel of a notification. */
public enum NotificationChannel {
  /** Electronic mail. */
  EMAIL,
  /** Short message service. */
  SMS,
  /** Push notification. */
  PUSH,
  /** In application feed. */
  IN_APP,
  /** Outbound webhook. */
  WEBHOOK
}
