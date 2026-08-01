package com.interviewintegrity.api;

import com.interviewintegrity.exception.AuthenticationFailedException;
import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.exception.DomainException;
import com.interviewintegrity.exception.ForbiddenException;
import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.exception.ValidationFailedException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Maps exceptions to their HTTP status and machine readable error code.
 *
 * <p>The mapping is intentionally small and stable so that every service exposes the same contract.
 */
public final class ApiErrorMappings {

  private ApiErrorMappings() {}

  /** Returns the HTTP status associated with the given throwable. */
  public static HttpStatus status(Throwable throwable) {
    if (throwable instanceof AuthenticationFailedException) {
      return HttpStatus.UNAUTHORIZED;
    }
    if (throwable instanceof ForbiddenException) {
      return HttpStatus.FORBIDDEN;
    }
    if (throwable instanceof NotFoundException) {
      return HttpStatus.NOT_FOUND;
    }
    if (throwable instanceof ConflictException) {
      return HttpStatus.CONFLICT;
    }
    if (throwable instanceof ValidationFailedException) {
      return HttpStatus.BAD_REQUEST;
    }
    if (throwable instanceof ResponseStatusException responseStatusException) {
      return HttpStatus.valueOf(responseStatusException.getStatusCode().value());
    }
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }

  /** Returns the stable machine readable code associated with the given throwable. */
  public static String code(Throwable throwable) {
    if (throwable instanceof DomainException domainException) {
      return domainException.code();
    }
    if (throwable instanceof ResponseStatusException) {
      return "REQUEST_REJECTED";
    }
    return "INTERNAL_ERROR";
  }

  /** Returns field violations for validation errors, empty otherwise. */
  public static List<FieldViolation> violations(Throwable throwable) {
    return List.of();
  }
}
