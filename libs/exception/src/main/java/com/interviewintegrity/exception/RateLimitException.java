package com.interviewintegrity.exception;

/** Raised when a caller exceeds an allowed request rate. */
public class RateLimitException extends DomainException {
  private static final long serialVersionUID = 1L;

  public RateLimitException(String message) {
    super("RATE_LIMITED", message);
  }
}
