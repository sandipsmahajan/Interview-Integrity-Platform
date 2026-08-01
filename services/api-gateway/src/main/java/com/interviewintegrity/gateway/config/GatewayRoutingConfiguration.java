package com.interviewintegrity.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the gateway routes for every microservice.
 *
 * <p>Routes are matched in declaration order, so the recruiter sub-routes under {@code
 * /api/v1/candidates/**} are declared before the candidate catch-all. The {@code
 * /api/v1/sessions/**} prefix is ambiguous between identity session management and the telemetry
 * sessions; the DELETE method is routed to identity and everything else to telemetry.
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
                    .filters(
                        f ->
                            f.requestRateLimiter(
                                c -> c.setRateLimiter(rateLimiter).setKeyResolver(keyResolver)))
                    .uri("http://localhost:8083"))
        .route(
            "identity",
            r ->
                r.path(
                        "/api/v1/auth/**",
                        "/api/v1/users/**",
                        "/api/v1/roles/**",
                        "/api/v1/permissions/**")
                    .filters(
                        f ->
                            f.requestRateLimiter(
                                c -> c.setRateLimiter(rateLimiter).setKeyResolver(keyResolver)))
                    .uri("http://localhost:8081"))
        .route(
            "identity-sessions",
            r -> r.method("DELETE").and().path("/api/v1/sessions/**").uri("http://localhost:8081"))
        .route(
            "organization",
            r ->
                r.path(
                        "/api/v1/organizations/**",
                        "/api/v1/plans/**",
                        "/api/v1/departments/**",
                        "/api/v1/teams/**")
                    .filters(
                        f ->
                            f.requestRateLimiter(
                                c -> c.setRateLimiter(rateLimiter).setKeyResolver(keyResolver)))
                    .uri("http://localhost:8082"))
        .route(
            "recruiter",
            r ->
                r.path("/api/v1/recruiters/**", "/api/v1/stages/**")
                    .filters(
                        f ->
                            f.requestRateLimiter(
                                c -> c.setRateLimiter(rateLimiter).setKeyResolver(keyResolver)))
                    .uri("http://localhost:8083"))
        .route(
            "candidate",
            r ->
                r.path("/api/v1/candidates/**", "/api/v1/tags/**")
                    .filters(
                        f ->
                            f.requestRateLimiter(
                                c -> c.setRateLimiter(rateLimiter).setKeyResolver(keyResolver)))
                    .uri("http://localhost:8084"))
        .route(
            "interview",
            r ->
                r.path("/api/v1/interviews/**", "/api/v1/interviewers/**")
                    .filters(
                        f ->
                            f.requestRateLimiter(
                                c -> c.setRateLimiter(rateLimiter).setKeyResolver(keyResolver)))
                    .uri("http://localhost:8085"))
        .route(
            "telemetry",
            r ->
                r.path("/api/v1/sessions/**", "/api/v1/event-types/**")
                    .filters(
                        f ->
                            f.requestRateLimiter(
                                c -> c.setRateLimiter(rateLimiter).setKeyResolver(keyResolver)))
                    .uri("http://localhost:8087"))
        .route(
            "policy-engine",
            r ->
                r.path("/api/v1/policies/**", "/api/v1/violations/**")
                    .filters(
                        f ->
                            f.requestRateLimiter(
                                c -> c.setRateLimiter(rateLimiter).setKeyResolver(keyResolver)))
                    .uri("http://localhost:8088"))
        .route(
            "report",
            r ->
                r.path("/api/v1/reports/**", "/api/v1/report-schedules/**")
                    .filters(
                        f ->
                            f.requestRateLimiter(
                                c -> c.setRateLimiter(rateLimiter).setKeyResolver(keyResolver)))
                    .uri("http://localhost:8089"))
        .route(
            "notification",
            r ->
                r.path(
                        "/api/v1/notifications/**",
                        "/api/v1/notification-templates/**",
                        "/api/v1/notification-preferences/**")
                    .filters(
                        f ->
                            f.requestRateLimiter(
                                c -> c.setRateLimiter(rateLimiter).setKeyResolver(keyResolver)))
                    .uri("http://localhost:8090"))
        .route(
            "analytics",
            r ->
                r.path("/api/v1/analytics/**")
                    .filters(
                        f ->
                            f.requestRateLimiter(
                                c -> c.setRateLimiter(rateLimiter).setKeyResolver(keyResolver)))
                    .uri("http://localhost:8091"))
        .route(
            "audit",
            r ->
                r.path("/api/v1/audit-events/**", "/api/v1/api-audit-log/**")
                    .filters(
                        f ->
                            f.requestRateLimiter(
                                c -> c.setRateLimiter(rateLimiter).setKeyResolver(keyResolver)))
                    .uri("http://localhost:8092"))
        .route(
            "storage",
            r ->
                r.path("/api/v1/buckets/**")
                    .filters(
                        f ->
                            f.requestRateLimiter(
                                c -> c.setRateLimiter(rateLimiter).setKeyResolver(keyResolver)))
                    .uri("http://localhost:8093"))
        .route(
            "feature-flag",
            r ->
                r.path("/api/v1/features/**", "/api/v1/experiments/**")
                    .filters(
                        f ->
                            f.requestRateLimiter(
                                c -> c.setRateLimiter(rateLimiter).setKeyResolver(keyResolver)))
                    .uri("http://localhost:8094"))
        .route(
            "scheduler",
            r ->
                r.path(
                        "/api/v1/scheduled-jobs/**",
                        "/api/v1/job-executions/**",
                        "/api/v1/job-locks/**")
                    .filters(
                        f ->
                            f.requestRateLimiter(
                                c -> c.setRateLimiter(rateLimiter).setKeyResolver(keyResolver)))
                    .uri("http://localhost:8095"))
        .route(
            "integration",
            r ->
                r.path(
                        "/api/v1/integrations/**",
                        "/api/v1/integration-connections/**",
                        "/api/v1/integration-webhooks/**",
                        "/api/v1/integration-sync-logs/**")
                    .filters(
                        f ->
                            f.requestRateLimiter(
                                c -> c.setRateLimiter(rateLimiter).setKeyResolver(keyResolver)))
                    .uri("http://localhost:8096"))
        .route(
            "configuration",
            r ->
                r.path("/api/v1/configurations/**", "/api/v1/configuration-schema/**")
                    .filters(
                        f ->
                            f.requestRateLimiter(
                                c -> c.setRateLimiter(rateLimiter).setKeyResolver(keyResolver)))
                    .uri("http://localhost:8097"))
        .build();
  }
}
