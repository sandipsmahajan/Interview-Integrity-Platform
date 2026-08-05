package com.integrity.common;

import java.time.Instant;

/** Default {@link TimeProvider} implementation backed by the system clock. */
public final class SystemTimeProvider implements TimeProvider {
  @Override
  public Instant now() {
    return Instant.now();
  }
}
