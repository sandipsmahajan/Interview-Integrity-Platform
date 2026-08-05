package com.integrity.organization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.integrity.organization.domain.Plan;
import com.integrity.organization.domain.Subscription;
import com.integrity.organization.repository.SubscriptionRepository;
import com.integrity.organization.web.dto.SubscribeRequest;
import com.integrity.organization.web.dto.SubscriptionResponse;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

/** Unit tests for the subscription lifecycle service. */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

  @Mock private SubscriptionRepository subscriptionRepository;
  @Mock private PlanService planService;
  private final OrganizationMapper mapper = new OrganizationMapperImpl();

  private SubscriptionService subscriptionService;

  @BeforeEach
  void setUp() {
    subscriptionService = new SubscriptionService(subscriptionRepository, planService, mapper);
  }

  @Test
  void subscribeCreatesTrialSubscriptionWhenNoneExists() {
    UUID organizationId = UUID.randomUUID();
    UUID byUser = UUID.randomUUID();
    Plan plan = plan("starter", "Starter");

    when(planService.requirePlanByCode("starter")).thenReturn(Mono.just(plan));
    when(subscriptionRepository.findByOrganizationId(organizationId)).thenReturn(Mono.empty());
    when(subscriptionRepository.save(any(Subscription.class)))
        .thenAnswer(
            invocation -> {
              Subscription subscription = invocation.getArgument(0);
              subscription.setId(UUID.randomUUID());
              return Mono.just(subscription);
            });
    when(planService.requirePlanById(any())).thenReturn(Mono.just(plan));

    SubscriptionResponse response =
        subscriptionService
            .subscribe(organizationId, byUser, new SubscribeRequest("starter", null))
            .block();

    assertThat(response).isNotNull();
    assertThat(response.planCode()).isEqualTo("starter");
    assertThat(response.status()).isEqualTo("TRIALING");
    assertThat(response.currentPeriodStart()).isBeforeOrEqualTo(LocalDate.now(ZoneOffset.UTC));
  }

  @Test
  void subscribeRenewsExistingSubscription() {
    UUID organizationId = UUID.randomUUID();
    UUID byUser = UUID.randomUUID();
    UUID planId = UUID.randomUUID();
    Plan plan = plan("starter", "Starter");
    Subscription existing =
        new Subscription(
            organizationId,
            planId,
            byUser,
            LocalDate.now(ZoneOffset.UTC).minusDays(10),
            LocalDate.now(ZoneOffset.UTC).plusDays(20));
    existing.setId(UUID.randomUUID());
    existing.markActive(byUser);

    when(planService.requirePlanByCode("starter")).thenReturn(Mono.just(plan));
    when(subscriptionRepository.findByOrganizationId(organizationId))
        .thenReturn(Mono.just(existing));
    when(subscriptionRepository.save(any(Subscription.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(planService.requirePlanById(planId)).thenReturn(Mono.just(plan));

    SubscriptionResponse response =
        subscriptionService
            .subscribe(organizationId, byUser, new SubscribeRequest("starter", null))
            .block();

    assertThat(response).isNotNull();
    assertThat(response.currentPeriodEnd()).isAfter(LocalDate.now(ZoneOffset.UTC).plusDays(15));
    verify(subscriptionRepository).save(existing);
  }

  private static Plan plan(String code, String name) {
    Plan plan = org.mockito.Mockito.mock(Plan.class);
    when(plan.getCode()).thenReturn(code);
    when(plan.getName()).thenReturn(name);
    return plan;
  }
}
