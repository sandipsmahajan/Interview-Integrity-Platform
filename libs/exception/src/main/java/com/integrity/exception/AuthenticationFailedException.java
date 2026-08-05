package com.integrity.exception;

/** Raised when a caller is not authenticated or its credentials are invalid. */
public class AuthenticationFailedException extends DomainException {
  private static final long serialVersionUID = 1L;

  public AuthenticationFailedException(String message) {
    super("UNAUTHENTICATED", message);
  }

  /** Creates an authentication failure wrapping the underlying cause. */
  public AuthenticationFailedException(String message, Throwable cause) {
    super("UNAUTHENTICATED", message, cause);
  }
}
