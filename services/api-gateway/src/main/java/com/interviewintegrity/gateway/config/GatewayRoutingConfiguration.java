package com.interviewintegrity.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the gateway routes for every microservice.
 *
 * <p>Routes are matched in declaration order, so the recruiter sub-routes under {@code
 * /api/v1/candidates/**} are declared before the candidate catch-all. The caller's own login
 * sessions are managed by identity under {@code /api/v1/auth/sessions/**}; interview session
 * lifecycle transitions under {@code /api/v1/sessions/**} for the pause, resume, complete and
 * abnormal operations are routed to interview; the remaining {@code /api/v1/sessions/**} prefix
 * belongs to telemetry sessions.
 *
 * <p>Downstream targets use the {@code lb://} scheme so instances are resolved from the service
 * registry (Eureka) by their {@code spring.application.name} instead of hardcoded host and port
 * values. Each route is guarded by a circuit breaker that forwards failures to {@code
 * /fallback/**}.
 */
@Configuration
public class GatewayRoutingConfiguration {

  /** Builds the ordered route table of the platform. */
  @Bean
  public RouteLocator routeLocator(
      RouteLocatorBuilder builder, RedisRateLimiter rateLimiter, KeyResolver keyResolver) {
    return builder
        .routes()
        .route(
            "recruiter-candidates",
            r ->
                r.path(
                        "/api/v1/candidates/*/pipeline/**",
                        "/api/v1/candidates/*/notes/**",
                        "/api/v1/candidates/*/assignments/**")
                    .filters(f -> routeFilters(f, rateLimiter, keyResolver, "recruiter-candidates"))
                    .uri("lb://recruiter-service"))
        .route(
            "identity",
            r ->
                r.path(
                        "/api/v1/auth/**",
                        "/api/v1/users/**",
                        "/api/v1/roles/**",
                        "/api/v1/permissions/**")
                    .filters(f -> routeFilters(f, rateLimiter, keyResolver, "identity"))
                    .uri("lb://identity-service"))
        .route(
            "identity-sessions",
            r ->
                r.path("/api/v1/auth/sessions/**")
                    .filters(f -> routeFilters(f, rateLimiter, keyResolver, "identity-sessions"))
                    .uri("lb://identity-service"))
        .route(
            "interview-session-lifecycle",
            r ->
                r.path(
                        "/api/v1/sessions/*/pause",
                        "/api/v1/sessions/*/resume",
                        "/api/v1/sessions/*/complete",
                        "/api/v1/sessions/*/abnormal")
                    .filters(
                        f ->
                            routeFilters(
                                f, rateLimiter, keyResolver, "interview-session-lifecycle"))
                    .uri("lb://interview-service"))
        .route(
            "organization",
            r ->
                r.path(
                        "/api/v1/organizations/**",
                        "/api/v1/plans/**",
                        "/api/v1/departments/**",
                        "/api/v1/teams/**")
                    .filters(f -> routeFilters(f, rateLimiter, keyResolver, "organization"))
                    .uri("lb://organization-service"))
        .route(
            "recruiter",
            r ->
                r.path("/api/v1/recruiters/**", "/api/v1/stages/**")
                    .filters(f -> routeFilters(f, rateLimiter, keyResolver, "recruiter"))
                    .uri("lb://recruiter-service"))
        .route(
            "candidate",
            r ->
                r.path("/api/v1/candidates/**", "/api/v1/tags/**")
                    .filters(f -> routeFilters(f, rateLimiter, keyResolver, "candidate"))
                    .uri("lb://candidate-service"))
        .route(
            "interview",
            r ->
                r.path("/api/v1/interviews/**", "/api/v1/interviewers/**")
                    .filters(f -> routeFilters(f, rateLimiter, keyResolver, "interview"))
                    .uri("lb://interview-service"))
        .route(
            "telemetry",
            r ->
                r.path("/api/v1/sessions/**", "/api/v1/event-types/**")
                    .filters(f -> routeFilters(f, rateLimiter, keyResolver, "telemetry"))
                    .uri("lb://telemetry-service"))
        .route(
            "policy-engine",
            r ->
                r.path("/api/v1/policies/**", "/api/v1/violations/**")
                    .filters(f -> routeFilters(f, rateLimiter, keyResolver, "policy-engine"))
                    .uri("lb://policy-engine-service"))
        .route(
            "report",
            r ->
                r.path("/api/v1/reports/**", "/api/v1/report-schedules/**")
                    .filters(f -> routeFilters(f, rateLimiter, keyResolver, "report"))
                    .uri("lb://report-service"))
        .route(
            "notification",
            r ->
                r.path(
                        "/api/v1/notifications/**",
                        "/api/v1/notification-templates/**",
                        "/api/v1/notification-preferences/**")
                    .filters(f -> routeFilters(f, rateLimiter, keyResolver, "notification"))
                    .uri("lb://notification-service"))
        .route(
            "analytics",
            r ->
                r.path("/api/v1/analytics/**")
                    .filters(f -> routeFilters(f, rateLimiter, keyResolver, "analytics"))
                    .uri("lb://analytics-service"))
        .route(
            "audit",
            r ->
                r.path("/api/v1/audit-events/**", "/api/v1/api-audit-log/**")
                    .filters(f -> routeFilters(f, rateLimiter, keyResolver, "audit"))
                    .uri("lb://audit-service"))
        .route(
            "storage",
            r ->
                r.path("/api/v1/buckets/**")
                    .filters(f -> routeFilters(f, rateLimiter, keyResolver, "storage"))
                    .uri("lb://storage-service"))
        .route(
            "feature-flag",
            r ->
                r.path("/api/v1/features/**", "/api/v1/experiments/**")
                    .filters(f -> routeFilters(f, rateLimiter, keyResolver, "feature-flag"))
                    .uri("lb://feature-flag-service"))
        .route(
            "scheduler",
            r ->
                r.path(
                        "/api/v1/scheduled-jobs/**",
                        "/api/v1/job-executions/**",
                        "/api/v1/job-locks/**")
                    .filters(f -> routeFilters(f, rateLimiter, keyResolver, "scheduler"))
                    .uri("lb://scheduler-service"))
        .route(
            "integration",
            r ->
                r.path(
                        "/api/v1/integrations/**",
                        "/api/v1/integration-connections/**",
                        "/api/v1/integration-webhooks/**",
                        "/api/v1/integration-sync-logs/**")
                    .filters(f -> routeFilters(f, rateLimiter, keyResolver, "integration"))
                    .uri("lb://integration-service"))
        .route(
            "configuration",
            r ->
                r.path("/api/v1/configurations/**", "/api/v1/configuration-schema/**")
                    .filters(f -> routeFilters(f, rateLimiter, keyResolver, "configuration"))
                    .uri("lb://configuration-service"))
        .build();
  }

  private GatewayFilterSpec routeFilters(
      GatewayFilterSpec filter,
      RedisRateLimiter rateLimiter,
      KeyResolver keyResolver,
      String circuitBreakerName) {
    return filter
        .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(keyResolver))
        .circuitBreaker(
            c -> c.setName(circuitBreakerName).setFallbackUri("forward:/fallback/{routeId}"));
  }
}
