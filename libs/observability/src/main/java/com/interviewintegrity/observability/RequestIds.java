package com.interviewintegrity.observability;

import java.util.UUID;

/** Constants and helpers for the request/correlation id used to trace a call across services. */
public final class RequestIds {

  /** Header used to propagate the request id between services. */
  public static final String HEADER_REQUEST_ID = "X-Request-Id";

  /** Legacy alias header accepted on ingress. */
  public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

  /** Key under which the request id is stored on the server exchange. */
  public static final String ATTRIBUTE_REQUEST_ID = "platform.requestId";

  private RequestIds() {}

  /** Generates a fresh request id. */
  public static String generate() {
    return UUID.randomUUID().toString();
  }
}
