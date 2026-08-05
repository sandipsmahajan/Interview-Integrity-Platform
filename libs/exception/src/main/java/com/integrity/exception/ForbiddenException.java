package com.integrity.exception;

/** Raised when an authenticated caller is not allowed to perform an operation. */
public class ForbiddenException extends DomainException {
  private static final long serialVersionUID = 1L;

  public ForbiddenException(String message) {
    super("FORBIDDEN", message);
  }
}
