package com.interviewintegrity.gateway.web;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Terminal fallback handler reached when a routed service is unavailable. */
@RestController
@RequestMapping("/fallback")
public final class FallbackController {

  /** Returns a 503 for any route that could not reach a downstream service. */
  @RequestMapping("/**")
  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  public Mono<Map<String, Object>> fallback() {
    return Mono.just(Map.of("status", "service_unavailable"));
  }
}
