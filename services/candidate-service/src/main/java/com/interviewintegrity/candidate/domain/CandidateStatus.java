package com.interviewintegrity.candidate.domain;

/** Lifecycle state of a candidate record. */
public enum CandidateStatus {
  /** Newly captured candidate, not yet screened. */
  NEW,
  /** Candidate is being screened by a recruiter. */
  SCREENING,
  /** Candidate is moving through interview rounds. */
  INTERVIEWING,
  /** An offer has been extended to the candidate. */
  OFFERED,
  /** The candidate has accepted an offer. */
  HIRED,
  /** The candidate was not progressed further. */
  REJECTED,
  /** The candidate is archived and no longer actively considered. */
  ARCHIVED
}
