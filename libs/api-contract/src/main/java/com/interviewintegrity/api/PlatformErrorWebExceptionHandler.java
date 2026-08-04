package com.interviewintegrity.api;

import com.interviewintegrity.observability.RequestIds;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/**
 * Reactive error handler that renders every failure as a {@link ErrorResponse} with the platform's
 * stable error contract.
 *
 * <p>Runs at {@link Ordered#HIGHEST_PRECEDENCE} so it always handles the exception before Spring
 * Boot's default {@code ErrorWebExceptionHandler} (which is ordered at {@code -1}); this is what
 * turns platform domain exceptions (e.g. {@code AuthenticationFailedException}) into their contract
 * status codes (e.g. 401) instead of a generic 500.
 */
public final class PlatformErrorWebExceptionHandler implements ErrorWebExceptionHandler, Ordered {

  private static final Logger log = LoggerFactory.getLogger(PlatformErrorWebExceptionHandler.class);

  private static final String GENERIC_SERVER_ERROR =
      "An unexpected internal error occurred. Contact support with the trace id.";

  private final ObjectMapper objectMapper;

  public PlatformErrorWebExceptionHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable throwable) {
    HttpStatus status = ApiErrorMappings.status(throwable);
    String code = ApiErrorMappings.code(throwable);
    List<FieldViolation> violations = ApiErrorMappings.violations(throwable);
    String traceId = exchange.getAttributeOrDefault(RequestIds.ATTRIBUTE_REQUEST_ID, "");
    ErrorResponse body =
        new ErrorResponse(
            status.value(), code, safeMessage(throwable), traceId, Instant.now(), violations);

    if (status.is5xxServerError()) {
      log.error("Unhandled error while processing request {}", traceId, throwable);
    } else if (log.isWarnEnabled()) {
      log.warn("Request {} failed with {}: {}", traceId, code, throwable.getMessage());
    }

    exchange.getResponse().setStatusCode(status);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    byte[] bytes = serialize(body);
    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
    return exchange.getResponse().writeWith(Mono.just(buffer));
  }

  private String safeMessage(Throwable throwable) {
    HttpStatus status = ApiErrorMappings.status(throwable);
    if (status.is5xxServerError()) {
      return GENERIC_SERVER_ERROR;
    }
    String message = throwable.getMessage();
    return message == null ? throwable.getClass().getSimpleName() : message;
  }

  private byte[] serialize(ErrorResponse body) {
    try {
      return objectMapper.writeValueAsBytes(body);
    } catch (Exception e) {
      String fallback =
          "{\"status\":500,\"code\":\"INTERNAL_ERROR\",\"message\":\"Unable to serialize error\","
              + "\"traceId\":\""
              + body.traceId()
              + "\",\"timestamp\":\""
              + body.timestamp()
              + "\",\"violations\":[]}";
      return fallback.getBytes(StandardCharsets.UTF_8);
    }
  }
}
