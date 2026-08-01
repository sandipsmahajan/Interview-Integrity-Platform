package com.interviewintegrity.organization.config;

import com.interviewintegrity.organization.service.DepartmentService;
import com.interviewintegrity.organization.service.OrganizationService;
import com.interviewintegrity.organization.service.PlanService;
import com.interviewintegrity.organization.service.SubscriptionService;
import com.interviewintegrity.organization.service.TeamService;
import com.interviewintegrity.organization.web.DepartmentController;
import com.interviewintegrity.organization.web.OrganizationAddressController;
import com.interviewintegrity.organization.web.OrganizationController;
import com.interviewintegrity.organization.web.OrganizationDomainController;
import com.interviewintegrity.organization.web.PlanController;
import com.interviewintegrity.organization.web.SubscriptionController;
import com.interviewintegrity.organization.web.TeamController;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the REST controllers as beans and describes the OpenAPI surface of the service. */
@Configuration
public class ApiConfiguration {

  /** Exposes the organization controller. */
  @Bean
  public OrganizationController organizationController(OrganizationService organizationService) {
    return new OrganizationController(organizationService);
  }

  /** Exposes the organization address controller. */
  @Bean
  public OrganizationAddressController organizationAddressController(
      OrganizationService organizationService) {
    return new OrganizationAddressController(organizationService);
  }

  /** Exposes the organization domain controller. */
  @Bean
  public OrganizationDomainController organizationDomainController(
      OrganizationService organizationService) {
    return new OrganizationDomainController(organizationService);
  }

  /** Exposes the department controller. */
  @Bean
  public DepartmentController departmentController(DepartmentService departmentService) {
    return new DepartmentController(departmentService);
  }

  /** Exposes the team controller. */
  @Bean
  public TeamController teamController(TeamService teamService) {
    return new TeamController(teamService);
  }

  /** Exposes the subscription controller. */
  @Bean
  public SubscriptionController subscriptionController(SubscriptionService subscriptionService) {
    return new SubscriptionController(subscriptionService);
  }

  /** Exposes the plan controller. */
  @Bean
  public PlanController planController(PlanService planService) {
    return new PlanController(planService);
  }

  /** Describes the OpenAPI document for the organization service. */
  @Bean
  public OpenAPI platformOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Organization Service API")
                .version("v1")
                .description("Tenant organization, department, team and subscription management"))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
