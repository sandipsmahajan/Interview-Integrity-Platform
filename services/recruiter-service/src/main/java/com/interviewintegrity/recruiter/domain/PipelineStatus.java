package com.interviewintegrity.recruiter.domain;

/** Movement state of a candidate within the hiring pipeline. */
public enum PipelineStatus {
  /** Candidate is currently in the stage. */
  CURRENT,
  /** Candidate has moved beyond or exited the stage. */
  PAST
}
