package com.interviewintegrity.organization.repository;

import com.interviewintegrity.organization.domain.Plan;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for the global {@link Plan} catalog. */
public interface PlanRepository extends ReactiveCrudRepository<Plan, UUID> {

  /** Finds a plan by its code. */
  Mono<Plan> findByCode(String code);

  /** Lists all plans ordered by monthly price. */
  @Query("SELECT * FROM plans ORDER BY monthly_price_cents")
  Flux<Plan> findAllOrdered();
}
