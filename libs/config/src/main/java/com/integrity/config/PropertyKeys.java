package com.integrity.config;

/** Common property key prefixes referenced by service configuration. */
public final class PropertyKeys {
  private PropertyKeys() {}

  /** Root prefix for platform-wide settings bound by {@code PlatformProperties}. */
  public static final String PLATFORM_PREFIX = "platform";

  /** Kafka bootstrap servers key ({@value #PLATFORM_PREFIX}.kafka.bootstrap-servers). */
  public static final String KAFKA_BOOTSTRAP = PLATFORM_PREFIX + ".kafka.bootstrap-servers";

  /** Object storage endpoint key ({@value #PLATFORM_PREFIX}.storage.endpoint). */
  public static final String STORAGE_ENDPOINT = PLATFORM_PREFIX + ".storage.endpoint";
}
