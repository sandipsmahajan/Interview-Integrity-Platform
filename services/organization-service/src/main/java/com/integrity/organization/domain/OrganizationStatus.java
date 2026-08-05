package com.integrity.organization.domain;

/** Lifecycle state of a tenant organization. */
public enum OrganizationStatus {
  /** Newly created organization before billing is confirmed. */
  TRIAL,
  /** Organization in good standing. */
  ACTIVE,
  /** Organization temporarily blocked due to compliance or billing issues. */
  SUSPENDED,
  /** Organization permanently closed. */
  CLOSED
}
