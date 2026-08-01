package com.interviewintegrity.organization.service;

import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.organization.domain.Plan;
import com.interviewintegrity.organization.repository.PlanRepository;
import com.interviewintegrity.organization.web.dto.PlanResponse;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Catalog operations over the global subscription plans. */
public final class PlanService {

  private final PlanRepository planRepository;

  /** Creates a service bound to the given repository. */
  public PlanService(PlanRepository planRepository) {
    this.planRepository = planRepository;
  }

  /** Lists all plans ordered by price. */
  public Flux<PlanResponse> listPlans() {
    return planRepository.findAllOrdered().map(this::toResponse);
  }

  /** Returns a single plan by code. */
  public Mono<PlanResponse> getPlanByCode(String code) {
    return planRepository
        .findByCode(code)
        .switchIfEmpty(Mono.error(new NotFoundException("Plan not found: " + code)))
        .map(this::toResponse);
  }

  private PlanResponse toResponse(Plan plan) {
    return new PlanResponse(
        plan.getId(),
        plan.getCode(),
        plan.getName(),
        plan.getMonthlyPriceCents(),
        plan.getMaxSeats(),
        plan.getFeatures());
  }

  /** Returns the raw plan for a code, used internally by the subscription service. */
  Mono<Plan> requirePlanByCode(String code) {
    return planRepository
        .findByCode(code)
        .switchIfEmpty(Mono.error(new NotFoundException("Plan not found: " + code)));
  }

  /** Returns the raw plan for an id, used internally by the subscription service. */
  Mono<Plan> requirePlanById(UUID planId) {
    return planRepository
        .findById(planId)
        .switchIfEmpty(Mono.error(new NotFoundException("Plan not found: " + planId)));
  }
}
