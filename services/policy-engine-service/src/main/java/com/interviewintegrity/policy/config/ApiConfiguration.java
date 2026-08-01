package com.interviewintegrity.policy.config;

import com.interviewintegrity.policy.service.PolicyEvaluationService;
import com.interviewintegrity.policy.service.PolicyRuleService;
import com.interviewintegrity.policy.service.PolicyService;
import com.interviewintegrity.policy.service.ViolationService;
import com.interviewintegrity.policy.web.PolicyController;
import com.interviewintegrity.policy.web.PolicyEvaluationController;
import com.interviewintegrity.policy.web.PolicyRuleController;
import com.interviewintegrity.policy.web.ViolationController;
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

  /** Exposes the policy controller. */
  @Bean
  public PolicyController policyController(PolicyService policyService) {
    return new PolicyController(policyService);
  }

  /** Exposes the rule controller. */
  @Bean
  public PolicyRuleController policyRuleController(PolicyRuleService ruleService) {
    return new PolicyRuleController(ruleService);
  }

  /** Exposes the violation controller. */
  @Bean
  public ViolationController violationController(ViolationService violationService) {
    return new ViolationController(violationService);
  }

  /** Exposes the evaluation controller. */
  @Bean
  public PolicyEvaluationController policyEvaluationController(
      PolicyEvaluationService evaluationService) {
    return new PolicyEvaluationController(evaluationService);
  }

  /** Describes the OpenAPI document for the policy engine service. */
  @Bean
  public OpenAPI platformOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Policy Engine Service API")
                .version("v1")
                .description("Integrity policies, rules and violation triage"))
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
