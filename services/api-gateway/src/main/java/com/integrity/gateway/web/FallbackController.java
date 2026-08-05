package com.integrity.gateway.web;

import com.integrity.api.ErrorResponse;
import com.integrity.observability.RequestIds;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Terminal fallback handler reached when a routed service is unavailable. */
@RestController
@RequestMapping("/fallback")
public final class FallbackController {

  /** Returns a 503 with the platform error contract for any unreachable downstream service. */
  @RequestMapping("/**")
  public Mono<ResponseEntity<ErrorResponse>> fallback(ServerWebExchange exchange) {
    String traceId = exchange.getAttributeOrDefault(RequestIds.ATTRIBUTE_REQUEST_ID, "");
    ErrorResponse body =
        new ErrorResponse(
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "SERVICE_UNAVAILABLE",
            "The requested service is temporarily unavailable, please retry later",
            traceId,
            Instant.now(),
            List.of());
    return Mono.just(
        ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body));
  }
}
