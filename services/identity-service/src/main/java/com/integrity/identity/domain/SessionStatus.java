package com.integrity.identity.domain;

/** Lifecycle status of a user session and its refresh token. */
public enum SessionStatus {
  /** Session current and refresh token valid. */
  ACTIVE,
  /** Session superseded by a newer refresh token. */
  REFRESHED,
  /** Session explicitly revoked. */
  REVOKED,
  /** Session expired by time. */
  EXPIRED
}
