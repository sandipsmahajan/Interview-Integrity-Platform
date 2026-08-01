package com.interviewintegrity.organization.web;

import com.interviewintegrity.organization.service.PlanService;
import com.interviewintegrity.organization.web.dto.PlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Read only endpoints over the global subscription plan catalog. */
@RestController
@RequestMapping("/api/v1/plans")
@Tag(name = "Plans", description = "Browse the subscription plan catalog")
public final class PlanController {

  private final PlanService planService;

  /** Creates the controller bound to the plan service. */
  public PlanController(PlanService planService) {
    this.planService = planService;
  }

  /** Lists all plans ordered by price. */
  @GetMapping
  @Operation(summary = "List plans")
  public Flux<PlanResponse> listPlans() {
    return planService.listPlans();
  }

  /** Returns a single plan by code. */
  @GetMapping("/{code}")
  @Operation(summary = "Get a plan")
  public Mono<PlanResponse> getPlan(@PathVariable String code) {
    return planService.getPlanByCode(code);
  }
}
