package com.interviewintegrity.platform.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewintegrity.platform.application.PlatformServices.DefaultPolicyEvaluationService;
import com.interviewintegrity.platform.application.PlatformServices.DefaultReportService;
import com.interviewintegrity.platform.application.PlatformServices.DefaultTelemetryCommandService;
import com.interviewintegrity.platform.application.PlatformServices.HmacTokenIssuer;
import com.interviewintegrity.platform.application.PlatformServices.PolicyEvaluationService;
import com.interviewintegrity.platform.application.PlatformServices.ReactiveEventPublisher;
import com.interviewintegrity.platform.application.PlatformServices.ReportService;
import com.interviewintegrity.platform.application.PlatformServices.TelemetryCommandService;
import com.interviewintegrity.platform.application.PlatformServices.TokenIssuer;
import com.interviewintegrity.platform.infrastructure.Repositories.TelemetryEventRepository;
import com.interviewintegrity.platform.infrastructure.Repositories.ViolationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ApplicationConfiguration {
  @Bean
  PolicyEvaluationService policyEvaluationService() {
    return new DefaultPolicyEvaluationService();
  }

  @Bean
  TelemetryCommandService telemetryCommandService(
      TelemetryEventRepository telemetryEvents,
      ViolationRepository violations,
      PolicyEvaluationService policy,
      ObjectMapper objectMapper,
      ReactiveEventPublisher publisher) {
    return new DefaultTelemetryCommandService(
        telemetryEvents, violations, policy, objectMapper, publisher);
  }

  @Bean
  ReportService reportService(ViolationRepository violations) {
    return new DefaultReportService(violations);
  }

  @Bean
  TokenIssuer tokenIssuer(
      @Value("${security.jwt.hmac-secret}") String secret, ObjectMapper objectMapper) {
    return new HmacTokenIssuer(secret, objectMapper);
  }
}
