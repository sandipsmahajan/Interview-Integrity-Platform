package com.interviewintegrity.organization.web;

import com.interviewintegrity.organization.service.SubscriptionService;
import com.interviewintegrity.organization.web.dto.SubscribeRequest;
import com.interviewintegrity.organization.web.dto.SubscriptionResponse;
import com.interviewintegrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Subscription endpoints for the caller's organization. */
@RestController
@RequestMapping("/api/v1/organizations/subscription")
@Tag(name = "Subscriptions", description = "Manage the organization subscription")
public final class SubscriptionController {

  private final SubscriptionService subscriptionService;

  /** Creates the controller bound to the subscription service. */
  public SubscriptionController(SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  /** Subscribes the organization to a plan. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Subscribe to a plan")
  public Mono<SubscriptionResponse> subscribe(
      Authentication authentication, @Valid @RequestBody SubscribeRequest request) {
    return subscriptionService.subscribe(
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication),
        request);
  }

  /** Returns the current subscription of the organization. */
  @GetMapping
  @Operation(summary = "Get my subscription")
  public Mono<SubscriptionResponse> getSubscription(Authentication authentication) {
    return subscriptionService.getSubscription(SecurityPrincipals.organizationId(authentication));
  }

  /** Schedules cancellation at the end of the current period. */
  @PostMapping("/cancel")
  @Operation(summary = "Cancel subscription")
  public Mono<SubscriptionResponse> cancelSubscription(Authentication authentication) {
    return subscriptionService.cancelSubscription(
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication));
  }

  /** Reverts a scheduled cancellation. */
  @PostMapping("/resume")
  @Operation(summary = "Resume subscription")
  public Mono<SubscriptionResponse> resumeSubscription(Authentication authentication) {
    return subscriptionService.resumeSubscription(
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication));
  }
}
