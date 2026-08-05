package com.integrity.exception;

/** Raised when a request conflicts with the current state of a resource. */
public class ConflictException extends DomainException {
  private static final long serialVersionUID = 1L;

  public ConflictException(String message) {
    super("CONFLICT", message);
  }
}
