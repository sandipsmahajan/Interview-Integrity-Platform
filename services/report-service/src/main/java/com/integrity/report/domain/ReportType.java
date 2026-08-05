package com.integrity.report.domain;

/** Subject area a report covers. */
public enum ReportType {
  /** A single proctoring session. */
  SESSION,
  /** A candidate over time. */
  CANDIDATE,
  /** A single interview. */
  INTERVIEW,
  /** A recruiter's activity. */
  RECRUITER,
  /** An organization wide overview. */
  ORGANIZATION,
  /** Integrity scorecard. */
  INTEGRITY
}
