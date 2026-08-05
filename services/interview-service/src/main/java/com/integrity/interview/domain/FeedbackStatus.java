package com.integrity.interview.domain;

/** State of the structured feedback collected for an interview. */
public enum FeedbackStatus {
  /** The feedback is still being written. */
  DRAFT,
  /** The feedback has been finalised and submitted. */
  SUBMITTED
}
