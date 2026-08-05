package com.integrity.organization.service;

import com.integrity.exception.NotFoundException;
import com.integrity.organization.domain.Plan;
import com.integrity.organization.repository.PlanRepository;
import com.integrity.organization.web.dto.PlanResponse;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Catalog operations over the global subscription plans. */
public final class PlanService {

  private final PlanRepository planRepository;
  private final OrganizationMapper mapper;

  /** Creates a service bound to the given repository. */
  public PlanService(PlanRepository planRepository, OrganizationMapper mapper) {
    this.planRepository = planRepository;
    this.mapper = mapper;
  }

  /** Lists all plans ordered by price. */
  public Flux<PlanResponse> listPlans() {
    return planRepository.findAllOrdered().map(mapper::toResponse);
  }

  /** Returns a single plan by code. */
  public Mono<PlanResponse> getPlanByCode(String code) {
    return planRepository
        .findByCode(code)
        .switchIfEmpty(Mono.error(new NotFoundException("Plan not found: " + code)))
        .map(mapper::toResponse);
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
