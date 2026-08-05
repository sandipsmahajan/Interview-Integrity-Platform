package com.integrity.organization.config;

import com.integrity.organization.repository.DepartmentRepository;
import com.integrity.organization.repository.OrganizationAddressRepository;
import com.integrity.organization.repository.OrganizationDomainRepository;
import com.integrity.organization.repository.OrganizationRepository;
import com.integrity.organization.repository.PlanRepository;
import com.integrity.organization.repository.SubscriptionRepository;
import com.integrity.organization.repository.TeamMemberRepository;
import com.integrity.organization.repository.TeamRepository;
import com.integrity.organization.service.DepartmentService;
import com.integrity.organization.service.KafkaOrganizationEventPublisher;
import com.integrity.organization.service.OrganizationEventPublisher;
import com.integrity.organization.service.OrganizationMapper;
import com.integrity.organization.service.OrganizationService;
import com.integrity.organization.service.PlanService;
import com.integrity.organization.service.SubscriptionService;
import com.integrity.organization.service.TeamService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.kafka.sender.KafkaSender;

/**
 * Explicit bean wiring for the organization service application services.
 *
 * <p>Services are plain classes instantiated here rather than discovered by component scanning,
 * keeping the dependency graph visible in one place.
 */
@Configuration
public class ApplicationConfiguration {

  /** Provides the database client backed team membership bridge repository. */
  @Bean
  public TeamMemberRepository teamMemberRepository(DatabaseClient databaseClient) {
    return new TeamMemberRepository(databaseClient);
  }

  /** Provides the event publisher for organization lifecycle events. */
  @Bean
  public OrganizationEventPublisher organizationEventPublisher(
      KafkaSender<String, String> sender, Environment environment) {
    String serviceName = environment.getProperty("spring.application.name", "organization-service");
    return new KafkaOrganizationEventPublisher(sender, serviceName);
  }

  /** Provides the organization service. */
  @Bean
  public OrganizationService organizationService(
      OrganizationRepository organizationRepository,
      OrganizationAddressRepository addressRepository,
      OrganizationDomainRepository domainRepository,
      OrganizationEventPublisher eventPublisher,
      OrganizationMapper mapper) {
    return new OrganizationService(
        organizationRepository, addressRepository, domainRepository, eventPublisher, mapper);
  }

  /** Provides the plan catalog service. */
  @Bean
  public PlanService planService(PlanRepository planRepository, OrganizationMapper mapper) {
    return new PlanService(planRepository, mapper);
  }

  /** Provides the subscription service. */
  @Bean
  public SubscriptionService subscriptionService(
      SubscriptionRepository subscriptionRepository,
      PlanService planService,
      OrganizationMapper mapper) {
    return new SubscriptionService(subscriptionRepository, planService, mapper);
  }

  /** Provides the department service. */
  @Bean
  public DepartmentService departmentService(
      DepartmentRepository departmentRepository, OrganizationMapper mapper) {
    return new DepartmentService(departmentRepository, mapper);
  }

  /** Provides the team service. */
  @Bean
  public TeamService teamService(
      TeamRepository teamRepository,
      DepartmentRepository departmentRepository,
      TeamMemberRepository teamMemberRepository,
      OrganizationMapper mapper) {
    return new TeamService(teamRepository, departmentRepository, teamMemberRepository, mapper);
  }
}
