package com.integrity.analytics.domain;

/** Lifecycle state of an analytics aggregation run. */
public enum JobRunStatus {
  /** The aggregation is in progress. */
  RUNNING,
  /** The aggregation completed successfully. */
  SUCCEEDED,
  /** The aggregation failed. */
  FAILED
}
