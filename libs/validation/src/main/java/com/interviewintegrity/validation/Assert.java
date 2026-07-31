package com.interviewintegrity.validation;

import com.interviewintegrity.exception.ValidationFailedException;

/** Defensive checks that raise {@link ValidationFailedException} when a precondition fails. */
public final class Assert {
  private Assert() {}

  /** Verifies the value is not {@code null}. */
  public static void notNull(Object value, String field) {
    if (value == null) {
      throw new ValidationFailedException(field + " must not be null");
    }
  }

  /** Verifies the value is not {@code null} and contains at least one non-whitespace character. */
  public static void notBlank(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new ValidationFailedException(field + " must not be blank");
    }
  }

  /** Verifies the condition holds, otherwise fails with the given message. */
  public static void isTrue(boolean condition, String message) {
    if (!condition) {
      throw new ValidationFailedException(message);
    }
  }
}
