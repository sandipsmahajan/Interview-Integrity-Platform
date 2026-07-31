package com.interviewintegrity.api;

import java.time.Instant;
import java.util.List;

/**
 * Standard error body returned by every service for failed requests.
 *
 * @param status HTTP status code
 * @param code stable machine-readable error code
 * @param message human readable error description
 * @param traceId correlation id that ties the error to a request trace
 * @param timestamp instant at which the error occurred
 * @param violations field level violations, empty when not applicable
 */
public record ErrorResponse(
    int status,
    String code,
    String message,
    String traceId,
    Instant timestamp,
    List<FieldViolation> violations) {}
