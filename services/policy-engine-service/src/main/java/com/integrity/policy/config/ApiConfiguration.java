package com.integrity.policy.config;

import com.integrity.policy.service.PolicyEvaluationService;
import com.integrity.policy.service.PolicyMapper;
import com.integrity.policy.service.PolicyRuleService;
import com.integrity.policy.service.PolicyService;
import com.integrity.policy.service.ViolationService;
import com.integrity.policy.web.PolicyController;
import com.integrity.policy.web.PolicyEvaluationController;
import com.integrity.policy.web.PolicyRuleController;
import com.integrity.policy.web.ViolationController;
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
  public PolicyController policyController(PolicyService policyService, PolicyMapper mapper) {
    return new PolicyController(policyService, mapper);
  }

  /** Exposes the rule controller. */
  @Bean
  public PolicyRuleController policyRuleController(
      PolicyRuleService ruleService, PolicyMapper mapper) {
    return new PolicyRuleController(ruleService, mapper);
  }

  /** Exposes the violation controller. */
  @Bean
  public ViolationController violationController(
      ViolationService violationService, PolicyMapper mapper) {
    return new ViolationController(violationService, mapper);
  }

  /** Exposes the evaluation controller. */
  @Bean
  public PolicyEvaluationController policyEvaluationController(
      PolicyEvaluationService evaluationService, PolicyMapper mapper) {
    return new PolicyEvaluationController(evaluationService, mapper);
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
