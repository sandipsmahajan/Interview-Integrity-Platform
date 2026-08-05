package com.integrity.config;

/** Well-known Spring profile names used across the platform. */
public final class PlatformProfiles {
  private PlatformProfiles() {}

  /** Local development profile (default when running from an IDE). */
  public static final String LOCAL = "local";

  /** Shared development environment profile. */
  public static final String DEV = "dev";

  /** CI and integration test profile. */
  public static final String TEST = "test";

  /** Staging profile used before production releases. */
  public static final String STAGING = "staging";

  /** Production profile. */
  public static final String PROD = "prod";
}
