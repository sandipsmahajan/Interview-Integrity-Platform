package com.integrity.organization.repository;

import com.integrity.organization.domain.Subscription;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link Subscription} entities. */
public interface SubscriptionRepository extends ReactiveCrudRepository<Subscription, UUID> {

  /** Finds the subscription of an organization. */
  Mono<Subscription> findByOrganizationId(UUID organizationId);

  /** Lists the subscriptions using a plan. */
  @Query("SELECT * FROM subscriptions WHERE plan_id = :planId ORDER BY created_at DESC")
  Flux<Subscription> listByPlan(UUID planId);
}
