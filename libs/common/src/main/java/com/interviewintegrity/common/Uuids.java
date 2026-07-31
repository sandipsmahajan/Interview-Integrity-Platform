package com.interviewintegrity.common;

import java.util.UUID;

/** Factory methods for universally unique identifiers. */
public final class Uuids {
  private Uuids() {}

  /** Returns a new random universally unique identifier. */
  public static UUID randomUuid() {
    return UUID.randomUUID();
  }
}
