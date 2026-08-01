package com.interviewintegrity.organization.domain;

/** Billing state of a tenant subscription. */
public enum SubscriptionStatus {
  /** Subscription in its free trial period. */
  TRIALING,
  /** Subscription is paid and current. */
  ACTIVE,
  /** Payment was attempted and failed, service continues for a grace window. */
  PAST_DUE,
  /** Subscription was cancelled but remains usable until the period ends. */
  CANCELED,
  /** Subscription is overdue and has lost access to paid features. */
  UNPAID
}
