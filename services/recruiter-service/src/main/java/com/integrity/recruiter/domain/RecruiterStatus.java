package com.integrity.recruiter.domain;

/** Working state of a recruiter profile. */
public enum RecruiterStatus {
  /** Actively recruiting. */
  ACTIVE,
  /** Temporarily not recruiting. */
  ON_LEAVE,
  /** No longer recruiting for this organization. */
  INACTIVE
}
