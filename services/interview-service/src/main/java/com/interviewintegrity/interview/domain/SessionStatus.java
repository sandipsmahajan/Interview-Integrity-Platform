package com.interviewintegrity.interview.domain;

/** Lifecycle state of a monitoring session. */
public enum SessionStatus {
  /** The session has been created but not started. */
  PENDING,
  /** The session is actively monitoring the interview. */
  ACTIVE,
  /** The session is temporarily suspended. */
  PAUSED,
  /** The session has finished normally. */
  ENDED,
  /** The session ended abnormally. */
  ABNORMAL
}
