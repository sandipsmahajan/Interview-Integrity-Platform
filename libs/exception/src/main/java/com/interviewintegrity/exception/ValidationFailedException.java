package com.interviewintegrity.exception;

/** Raised when a request carries invalid or unprocessable input. */
public class ValidationFailedException extends DomainException {
  private static final long serialVersionUID = 1L;

  public ValidationFailedException(String message) {
    super("VALIDATION_FAILED", message);
  }
}
