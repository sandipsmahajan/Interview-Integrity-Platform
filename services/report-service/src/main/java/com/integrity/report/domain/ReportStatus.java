package com.integrity.report.domain;

/** Lifecycle state of a generated report. */
public enum ReportStatus {
  /** Awaiting generation. */
  REQUESTED,
  /** Generation is in progress. */
  GENERATING,
  /** Available for download. */
  READY,
  /** Generation failed. */
  FAILED,
  /** No longer available. */
  EXPIRED
}
