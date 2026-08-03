package com.interviewintegrity.common;

import org.springframework.r2dbc.core.DatabaseClient;

/** Helpers for binding nullable values in {@link DatabaseClient} statements. */
public final class R2dbcBindings {
  private R2dbcBindings() {}

  /**
   * Binds the value, or a typed SQL null when it is {@code null}. This keeps optional columns
   * NULL-able instead of binding a JDBC/R2DBC default that Postgres would reject.
   */
  public static DatabaseClient.GenericExecuteSpec bindOrNull(
      DatabaseClient.GenericExecuteSpec spec, String name, Object value, Class<?> type) {
    if (value == null) {
      return spec.bindNull(name, type);
    }
    return spec.bind(name, value);
  }
}
