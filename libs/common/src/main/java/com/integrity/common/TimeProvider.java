package com.integrity.common;

import java.time.Instant;

/** Provides the current point in time so that domain logic can use a controllable clock. */
@FunctionalInterface
public interface TimeProvider {
  /** Returns the current instant. */
  Instant now();
}
