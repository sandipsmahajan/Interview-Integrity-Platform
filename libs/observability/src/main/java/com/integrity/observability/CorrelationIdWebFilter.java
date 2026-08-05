package com.integrity.observability;

import com.integrity.logging.MdcKeys;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

/**
 * Reactive filter that ensures every request carries a request id.
 *
 * <p>The id is taken from the {@code X-Request-Id} header when present, otherwise generated. It is
 * echoed back on the response, stored on the exchange, placed in the MDC for structured logging,
 * and published into the Reactor context for downstream propagation.
 */
public final class CorrelationIdWebFilter implements WebFilter {

  /** Reactor context key under which the request id is published. */
  public static final String CONTEXT_REQUEST_ID = "platform.requestId";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String requestId = resolveRequestId(exchange);
    exchange.getAttributes().put(RequestIds.ATTRIBUTE_REQUEST_ID, requestId);
    exchange.getResponse().getHeaders().set(RequestIds.HEADER_REQUEST_ID, requestId);
    MDC.put(MdcKeys.REQUEST_ID, requestId);
    MDC.put(MdcKeys.TRACE_ID, requestId);
    return chain
        .filter(exchange)
        .contextWrite(context -> context.put(CONTEXT_REQUEST_ID, requestId))
        .doFinally(
            signal -> {
              MDC.remove(MdcKeys.REQUEST_ID);
              MDC.remove(MdcKeys.TRACE_ID);
            });
  }

  private String resolveRequestId(ServerWebExchange exchange) {
    HttpHeaders headers = exchange.getRequest().getHeaders();
    String fromHeader = headers.getFirst(RequestIds.HEADER_REQUEST_ID);
    if (fromHeader == null || fromHeader.isBlank()) {
      fromHeader = headers.getFirst(RequestIds.HEADER_CORRELATION_ID);
    }
    if (fromHeader != null && !fromHeader.isBlank()) {
      return fromHeader;
    }
    Object attribute = exchange.getAttribute(RequestIds.ATTRIBUTE_REQUEST_ID);
    if (attribute instanceof String existing && !existing.isBlank()) {
      return existing;
    }
    return RequestIds.generate();
  }

  /** Reads the request id from the Reactor context view, or {@code null} when absent. */
  public static String fromContext(ContextView contextView) {
    return contextView.getOrDefault(CONTEXT_REQUEST_ID, null);
  }
}
