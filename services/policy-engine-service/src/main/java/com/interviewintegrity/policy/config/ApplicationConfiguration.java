package com.interviewintegrity.policy.config;

import com.interviewintegrity.policy.repository.PolicyRepository;
import com.interviewintegrity.policy.repository.PolicyRuleRepository;
import com.interviewintegrity.policy.repository.PolicyVersionRepository;
import com.interviewintegrity.policy.repository.ViolationEscalationRepository;
import com.interviewintegrity.policy.repository.ViolationRepository;
import com.interviewintegrity.policy.repository.ViolationReviewRepository;
import com.interviewintegrity.policy.service.PolicyEvaluationService;
import com.interviewintegrity.policy.service.PolicyPublishingService;
import com.interviewintegrity.policy.service.PolicyRuleService;
import com.interviewintegrity.policy.service.PolicyService;
import com.interviewintegrity.policy.service.ViolationConsumer;
import com.interviewintegrity.policy.service.ViolationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.kafka.receiver.KafkaReceiver;

/**
 * Explicit bean wiring for the policy engine service application services.
 *
 * <p>Services are plain classes instantiated here rather than discovered by component scanning,
 * keeping the dependency graph visible in one place.
 */
@Configuration
public class ApplicationConfiguration {

  /** Provides the policy repository. */
  @Bean
  public PolicyRepository policyRepository(DatabaseClient databaseClient) {
    return new PolicyRepository(databaseClient);
  }

  /** Provides the policy rule repository. */
  @Bean
  public PolicyRuleRepository policyRuleRepository(DatabaseClient databaseClient) {
    return new PolicyRuleRepository(databaseClient);
  }

  /** Provides the policy version repository. */
  @Bean
  public PolicyVersionRepository policyVersionRepository(DatabaseClient databaseClient) {
    return new PolicyVersionRepository(databaseClient);
  }

  /** Provides the violation repository. */
  @Bean
  public ViolationRepository violationRepository(DatabaseClient databaseClient) {
    return new ViolationRepository(databaseClient);
  }

  /** Provides the violation review repository. */
  @Bean
  public ViolationReviewRepository violationReviewRepository(DatabaseClient databaseClient) {
    return new ViolationReviewRepository(databaseClient);
  }

  /** Provides the violation escalation repository. */
  @Bean
  public ViolationEscalationRepository violationEscalationRepository(
      DatabaseClient databaseClient) {
    return new ViolationEscalationRepository(databaseClient);
  }

  /** Provides the policy service. */
  @Bean
  public PolicyService policyService(
      PolicyRepository policyRepository, PolicyPublishingService publishingService) {
    return new PolicyService(policyRepository, publishingService);
  }

  /** Provides the policy rule service. */
  @Bean
  public PolicyRuleService policyRuleService(
      PolicyRuleRepository ruleRepository, PolicyService policyService) {
    return new PolicyRuleService(ruleRepository, policyService);
  }

  /** Provides the policy publishing service. */
  @Bean
  public PolicyPublishingService policyPublishingService(
      PolicyRuleRepository ruleRepository, PolicyVersionRepository versionRepository) {
    return new PolicyPublishingService(ruleRepository, versionRepository);
  }

  /** Provides the violation service. */
  @Bean
  public ViolationService violationService(
      ViolationRepository violationRepository,
      ViolationReviewRepository reviewRepository,
      ViolationEscalationRepository escalationRepository) {
    return new ViolationService(violationRepository, reviewRepository, escalationRepository);
  }

  /** Provides the rule evaluator. */
  @Bean
  public PolicyEvaluationService policyEvaluationService(
      PolicyRuleService ruleService, ViolationService violationService) {
    return new PolicyEvaluationService(ruleService, violationService);
  }

  /** Provides the topic consumer and starts its subscription on startup. */
  @Bean
  public ViolationConsumer violationConsumer(
      KafkaReceiver<String, String> receiver, ViolationService violationService) {
    ViolationConsumer consumer = new ViolationConsumer(receiver, violationService);
    consumer.start();
    return consumer;
  }
}
