package com.integrity.candidate.domain;

/** Lifecycle state of an assessment assigned to a candidate. */
public enum AssessmentStatus {
  /** Assessment has been assigned but not yet started. */
  ASSIGNED,
  /** Candidate has begun working on the assessment. */
  IN_PROGRESS,
  /** Assessment has been submitted and scored. */
  COMPLETED,
  /** Assessment link has lapsed without completion. */
  EXPIRED
}
