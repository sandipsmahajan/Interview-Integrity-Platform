package com.interviewintegrity.exception;

/** Raised when a requested resource does not exist. */
public class NotFoundException extends DomainException {
  private static final long serialVersionUID = 1L;

  public NotFoundException(String message) {
    super("RESOURCE_NOT_FOUND", message);
  }
}
