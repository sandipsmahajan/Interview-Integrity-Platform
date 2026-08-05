package com.integrity.audit.domain;

/** Result of the audited action. */
public enum AuditOutcome {
  /** The action completed successfully. */
  SUCCESS,
  /** The action failed while executing. */
  FAILURE,
  /** The action was refused by a policy or permission check. */
  DENIED
}
