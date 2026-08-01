package com.interviewintegrity.policy.domain;

/** Triage state of a detected integrity violation. */
public enum ViolationStatus {
  OPEN,
  IN_REVIEW,
  ESCALATED,
  RESOLVED,
  DISMISSED
}
