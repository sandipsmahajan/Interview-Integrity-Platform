package com.interviewintegrity.scheduler.domain;

/** Lifecycle state of a job execution attempt. */
public enum ExecutionStatus {
  RUNNING,
  SUCCEEDED,
  FAILED,
  TIMED_OUT,
  SKIPPED
}
