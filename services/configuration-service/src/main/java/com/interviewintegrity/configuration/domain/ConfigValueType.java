package com.interviewintegrity.configuration.domain;

/** Supported value types of a configuration key. */
public enum ConfigValueType {
  /** String value. */
  STRING,
  /** Numeric value. */
  NUMBER,
  /** Boolean value. */
  BOOLEAN,
  /** JSON object value. */
  JSON,
  /** Duration value. */
  DURATION
}
