package com.interviewintegrity.exception;

/** Base class for all domain exceptions raised by the platform's business logic. */
public abstract class DomainException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final String errorCode;

  protected DomainException(String code, String message) {
    super(message);
    this.errorCode = code;
  }

  protected DomainException(String code, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = code;
  }

  /** Stable machine-readable error code that can be surfaced on the API contract. */
  public final String code() {
    return errorCode;
  }
}
