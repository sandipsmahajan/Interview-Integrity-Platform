package com.interviewintegrity.featureflag.domain;

/** Lifecycle state of an A/B experiment. */
public enum ExperimentStatus {
  /** Created but not yet started. */
  DRAFT,
  /** Actively allocating users between variants. */
  RUNNING,
  /** Temporarily stopped, can be resumed. */
  PAUSED,
  /** Finished and ready to be evaluated. */
  COMPLETED,
  /** Rejected before producing results. */
  REJECTED
}
