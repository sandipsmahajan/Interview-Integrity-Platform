package com.integrity.interview.domain;

/** Lifecycle state of an interview. */
public enum InterviewStatus {
  /** The interview has been booked but not yet started. */
  SCHEDULED,
  /** A monitoring session is currently running the interview. */
  IN_PROGRESS,
  /** The interview has finished. */
  COMPLETED,
  /** The interview was cancelled before it started. */
  CANCELLED,
  /** The candidate did not attend. */
  NO_SHOW
}
