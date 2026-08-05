package com.integrity.featureflag.domain;

/** Data type of a feature flag. */
public enum FlagKind {
  /** Boolean on/off flag. */
  BOOLEAN,
  /** String variant flag. */
  STRING,
  /** Numeric variant flag. */
  NUMBER,
  /** JSON payload flag. */
  JSON
}
