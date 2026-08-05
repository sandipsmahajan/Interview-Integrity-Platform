package com.integrity.candidate.domain;

/** Grant state of a data-protection consent. */
public enum ConsentStatus {
  /** The consent is active. */
  GRANTED,
  /** The consent has been withdrawn. */
  REVOKED
}
