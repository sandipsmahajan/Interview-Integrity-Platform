package com.integrity.notification.domain;

/** Lifecycle state of a notification. */
public enum NotificationStatus {
  /** Awaiting dispatch. */
  PENDING,
  /** Dispatched to the provider. */
  SENT,
  /** Confirmed delivered by the provider. */
  DELIVERED,
  /** Opened by the recipient. */
  READ,
  /** Dispatch failed. */
  FAILED
}
