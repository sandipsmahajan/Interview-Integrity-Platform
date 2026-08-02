package com.interviewintegrity.exception;

import java.util.List;

/** Raised when a request carries invalid or unprocessable input. */
public class ValidationFailedException extends DomainException {
  private static final long serialVersionUID = 1L;

  @SuppressWarnings({"serial", "PMD.AvoidFieldNameMatchingMethodName"})
  private final List<Violation> violations;

  public ValidationFailedException(String message) {
    super("VALIDATION_FAILED", message);
    this.violations = List.of();
  }

  /**
   * Creates a validation failure with structured field violations.
   *
   * @param message human readable summary of the failure
   * @param violations field level violations contributing to the failure
   */
  public ValidationFailedException(String message, List<Violation> violations) {
    super("VALIDATION_FAILED", message);
    this.violations = List.copyOf(violations);
  }

  /** Returns the field level violations, empty when only the summary message applies. */
  public List<Violation> violations() {
    return violations;
  }
}
