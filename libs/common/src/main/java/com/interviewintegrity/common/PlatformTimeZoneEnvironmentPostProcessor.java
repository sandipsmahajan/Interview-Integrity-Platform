package com.interviewintegrity.common;

import java.util.TimeZone;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

/**
 * Applies the platform timezone before the application context is created.
 *
 * <p>PostgreSQL drivers (pgJDBC and r2dbc-postgresql) derive the {@code TimeZone} startup parameter
 * from the JVM default timezone and ignore connection-string overrides. Running on a host whose
 * default timezone is not accepted by the server (for example an unknown {@code tzdata} database)
 * fails startup with {@code FATAL: invalid value for parameter "TimeZone"}.
 *
 * <p>This processor reads {@code app.timezone} (environment variable {@code APP_TIMEZONE}) and sets
 * the JVM default before any bean, connection pool or Flyway instance is created. It runs with the
 * default order, i.e. after {@code ConfigDataEnvironmentPostProcessor}, so values from {@code
 * application.yml} are already visible.
 */
public class PlatformTimeZoneEnvironmentPostProcessor implements EnvironmentPostProcessor {

  /** Property used to select the platform timezone. */
  public static final String TIMEZONE_PROPERTY = "app.timezone";

  /** Environment variable that maps to {@link #TIMEZONE_PROPERTY}. */
  public static final String TIMEZONE_ENV = "APP_TIMEZONE";

  /** Default timezone used when the property is not configured. */
  public static final String DEFAULT_TIMEZONE = "UTC";

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    String zoneId = environment.getProperty(TIMEZONE_PROPERTY, DEFAULT_TIMEZONE);
    if (!StringUtils.hasText(zoneId)) {
      zoneId = DEFAULT_TIMEZONE;
    }
    TimeZone zone = TimeZone.getTimeZone(zoneId);
    if (!zone.getID().equals(zoneId)) {
      throw new IllegalStateException(
          "Unknown timezone '"
              + zoneId
              + "' for "
              + TIMEZONE_PROPERTY
              + ". "
              + "Use a valid java.util.TimeZone ID, e.g. UTC, Asia/Kolkata, Europe/London.");
    }
    TimeZone.setDefault(zone);
  }
}
