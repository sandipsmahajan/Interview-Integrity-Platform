package com.integrity.api;

import com.integrity.exception.AuthenticationFailedException;
import com.integrity.exception.ConflictException;
import com.integrity.exception.DomainException;
import com.integrity.exception.ForbiddenException;
import com.integrity.exception.NotFoundException;
import com.integrity.exception.RateLimitException;
import com.integrity.exception.ValidationFailedException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.support.WebExchangeBindException;
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
    if (throwable instanceof RateLimitException) {
      return HttpStatus.TOO_MANY_REQUESTS;
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
    if (throwable instanceof ValidationFailedException validationException) {
      return validationException.violations().stream()
          .map(violation -> new FieldViolation(violation.field(), violation.message()))
          .toList();
    }
    if (throwable instanceof WebExchangeBindException bindException) {
      return bindException.getFieldErrors().stream()
          .map(
              fieldError ->
                  new FieldViolation(fieldError.getField(), fieldError.getDefaultMessage()))
          .toList();
    }
    return List.of();
  }
}
