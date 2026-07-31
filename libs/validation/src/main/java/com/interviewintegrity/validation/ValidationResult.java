package com.interviewintegrity.validation;

import java.util.List;

/**
 * Immutable outcome of a validation pass.
 *
 * @param errors human readable messages describing why validation failed
 */
public record ValidationResult(List<String> errors) {
  private static final ValidationResult VALID = new ValidationResult(List.of());

  /** Returns a result that contains no errors. */
  public static ValidationResult valid() {
    return VALID;
  }

  /** Returns a result with the given error messages. */
  public static ValidationResult invalid(List<String> errors) {
    return new ValidationResult(List.copyOf(errors));
  }

  /** Returns {@code true} when no validation errors were recorded. */
  public boolean isValid() {
    return errors.isEmpty();
  }
}
