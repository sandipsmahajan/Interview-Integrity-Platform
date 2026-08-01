package com.interviewintegrity.organization.config;

import com.interviewintegrity.organization.repository.DepartmentRepository;
import com.interviewintegrity.organization.repository.OrganizationAddressRepository;
import com.interviewintegrity.organization.repository.OrganizationDomainRepository;
import com.interviewintegrity.organization.repository.OrganizationRepository;
import com.interviewintegrity.organization.repository.PlanRepository;
import com.interviewintegrity.organization.repository.SubscriptionRepository;
import com.interviewintegrity.organization.repository.TeamMemberRepository;
import com.interviewintegrity.organization.repository.TeamRepository;
import com.interviewintegrity.organization.service.DepartmentService;
import com.interviewintegrity.organization.service.KafkaOrganizationEventPublisher;
import com.interviewintegrity.organization.service.OrganizationEventPublisher;
import com.interviewintegrity.organization.service.OrganizationService;
import com.interviewintegrity.organization.service.PlanService;
import com.interviewintegrity.organization.service.SubscriptionService;
import com.interviewintegrity.organization.service.TeamService;
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
      OrganizationEventPublisher eventPublisher) {
    return new OrganizationService(
        organizationRepository, addressRepository, domainRepository, eventPublisher);
  }

  /** Provides the plan catalog service. */
  @Bean
  public PlanService planService(PlanRepository planRepository) {
    return new PlanService(planRepository);
  }

  /** Provides the subscription service. */
  @Bean
  public SubscriptionService subscriptionService(
      SubscriptionRepository subscriptionRepository, PlanService planService) {
    return new SubscriptionService(subscriptionRepository, planService);
  }

  /** Provides the department service. */
  @Bean
  public DepartmentService departmentService(DepartmentRepository departmentRepository) {
    return new DepartmentService(departmentRepository);
  }

  /** Provides the team service. */
  @Bean
  public TeamService teamService(
      TeamRepository teamRepository,
      DepartmentRepository departmentRepository,
      TeamMemberRepository teamMemberRepository) {
    return new TeamService(teamRepository, departmentRepository, teamMemberRepository);
  }
}
