package com.interviewintegrity.organization.service;

import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.organization.domain.Plan;
import com.interviewintegrity.organization.domain.Subscription;
import com.interviewintegrity.organization.domain.SubscriptionStatus;
import com.interviewintegrity.organization.repository.SubscriptionRepository;
import com.interviewintegrity.organization.web.dto.SubscribeRequest;
import com.interviewintegrity.organization.web.dto.SubscriptionResponse;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import reactor.core.publisher.Mono;

/** Lifecycle operations over the single organization subscription. */
public final class SubscriptionService {

  private static final int TRIAL_DAYS = 30;

  private final SubscriptionRepository subscriptionRepository;
  private final PlanService planService;
  private final OrganizationMapper mapper;

  /** Creates a service bound to the given repository and plan catalog. */
  public SubscriptionService(
      SubscriptionRepository subscriptionRepository,
      PlanService planService,
      OrganizationMapper mapper) {
    this.subscriptionRepository = subscriptionRepository;
    this.planService = planService;
    this.mapper = mapper;
  }

  /** Subscribes the organization to the requested plan, creating or switching the subscription. */
  public Mono<SubscriptionResponse> subscribe(
      UUID organizationId, UUID byUser, SubscribeRequest request) {
    return planService
        .requirePlanByCode(request.planCode())
        .flatMap(plan -> upsertSubscription(organizationId, byUser, plan));
  }

  /** Returns the current subscription of the organization. */
  public Mono<SubscriptionResponse> getSubscription(UUID organizationId) {
    return subscriptionRepository
        .findByOrganizationId(organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Organization has no subscription")))
        .flatMap(this::withPlan);
  }

  /** Schedules cancellation at the end of the current period. */
  public Mono<SubscriptionResponse> cancelSubscription(UUID organizationId, UUID byUser) {
    return subscriptionRepository
        .findByOrganizationId(organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Organization has no subscription")))
        .flatMap(
            subscription -> {
              subscription.scheduleCancellation(byUser);
              return subscriptionRepository.save(subscription);
            })
        .flatMap(this::withPlan);
  }

  /** Reverts a scheduled cancellation and reactivates the subscription. */
  public Mono<SubscriptionResponse> resumeSubscription(UUID organizationId, UUID byUser) {
    return subscriptionRepository
        .findByOrganizationId(organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Organization has no subscription")))
        .flatMap(
            subscription -> {
              subscription.resume(byUser);
              return subscriptionRepository.save(subscription);
            })
        .flatMap(this::withPlan);
  }

  private Mono<SubscriptionResponse> upsertSubscription(
      UUID organizationId, UUID byUser, Plan plan) {
    return subscriptionRepository
        .findByOrganizationId(organizationId)
        .flatMap(
            existing -> {
              existing.renew(today(), today().plusDays(TRIAL_DAYS));
              if (existing.getStatus() == SubscriptionStatus.UNPAID) {
                existing.resume(byUser);
              }
              return subscriptionRepository.save(existing);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  LocalDate start = today();
                  Subscription created =
                      new Subscription(
                          organizationId, plan.getId(), byUser, start, start.plusDays(TRIAL_DAYS));
                  return subscriptionRepository.save(created);
                }))
        .flatMap(this::withPlan);
  }

  private static LocalDate today() {
    return LocalDate.now(ZoneOffset.UTC);
  }

  private Mono<SubscriptionResponse> withPlan(Subscription subscription) {
    return planService
        .requirePlanById(subscription.getPlanId())
        .map(plan -> mapper.toResponse(subscription, plan));
  }
}
