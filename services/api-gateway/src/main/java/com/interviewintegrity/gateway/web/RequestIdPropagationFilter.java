package com.interviewintegrity.gateway.web;

import com.interviewintegrity.observability.RequestIds;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Propagates the request correlation id to downstream services.
 *
 * <p>The inbound request id is resolved by the platform {@code CorrelationIdWebFilter} and stored
 * on the exchange. When the caller did not provide one, that filter generates the id but never
 * writes it to the outgoing request. This filter copies the resolved id into the outbound {@code
 * X-Request-Id} header so gateway-generated ids reach every {@code lb://} target and the whole call
 * chain shares one correlation id.
 */
@Component
public final class RequestIdPropagationFilter implements GlobalFilter, Ordered {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String requestId = exchange.getAttribute(RequestIds.ATTRIBUTE_REQUEST_ID);
    if (requestId == null
        || exchange.getRequest().getHeaders().getFirst(RequestIds.HEADER_REQUEST_ID) != null) {
      return chain.filter(exchange);
    }
    ServerWebExchange mutated =
        exchange
            .mutate()
            .request(
                request ->
                    request.headers(
                        headers -> headers.set(RequestIds.HEADER_REQUEST_ID, requestId)))
            .build();
    return chain.filter(mutated);
  }

  /** Runs before the routing filter so the header is in place when the downstream call is made. */
  @Override
  public int getOrder() {
    return -1;
  }
}
