package com.integrity.identity.domain;

/** Lifecycle status of a platform user account. */
public enum UserStatus {
  /** Account created but not yet activated. */
  PENDING,
  /** Account active and able to authenticate. */
  ACTIVE,
  /** Account disabled by an administrator. */
  DISABLED,
  /** Account locked after repeated failed authentication attempts. */
  LOCKED
}
