package com.interviewintegrity.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configures the distributed rate limiting used by the gateway routes.
 *
 * <p>The limiter keys on the authenticated principal when available, otherwise falls back to the
 * caller IP address, so anonymous callers are throttled per source address while signed-in users
 * are throttled per identity.
 */
@Configuration
public class GatewayRateLimiterConfiguration {

  /** Provides the token bucket rate limiter backed by Redis. */
  @Bean
  @Primary
  public RedisRateLimiter redisRateLimiter() {
    return new RedisRateLimiter(20, 40, 1);
  }

  /** Provides the key resolver used to bucket requests for rate limiting. */
  @Bean
  public KeyResolver gatewayKeyResolver() {
    return exchange ->
        exchange
            .getPrincipal()
            .map(principal -> principal.getName())
            .defaultIfEmpty(resolveClientAddress(exchange));
  }

  private String resolveClientAddress(org.springframework.web.server.ServerWebExchange exchange) {
    if (exchange.getRequest().getRemoteAddress() == null) {
      return "anonymous";
    }
    String address = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    return address == null ? "anonymous" : address;
  }
}
