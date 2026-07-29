package com.interviewintegrity.platform.api;

import com.interviewintegrity.platform.api.PlatformControllers.AuthController;
import com.interviewintegrity.platform.api.PlatformControllers.InterviewController;
import com.interviewintegrity.platform.api.PlatformControllers.NotificationController;
import com.interviewintegrity.platform.api.PlatformControllers.ReportController;
import com.interviewintegrity.platform.api.PlatformControllers.SessionController;
import com.interviewintegrity.platform.api.PlatformControllers.TelemetryController;
import com.interviewintegrity.platform.application.PlatformServices.ReportService;
import com.interviewintegrity.platform.application.PlatformServices.TelemetryCommandService;
import com.interviewintegrity.platform.application.PlatformServices.TokenIssuer;
import com.interviewintegrity.platform.infrastructure.Repositories.InterviewRepository;
import com.interviewintegrity.platform.infrastructure.Repositories.InterviewSessionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ApiConfiguration {
  @Bean
  AuthController authController(TokenIssuer tokenIssuer) {
    return new AuthController(tokenIssuer);
  }

  @Bean
  InterviewController interviewController(InterviewRepository interviews) {
    return new InterviewController(interviews);
  }

  @Bean
  SessionController sessionController(InterviewSessionRepository sessions) {
    return new SessionController(sessions);
  }

  @Bean
  TelemetryController telemetryController(TelemetryCommandService telemetry) {
    return new TelemetryController(telemetry);
  }

  @Bean
  ReportController reportController(ReportService reports) {
    return new ReportController(reports);
  }

  @Bean
  NotificationController notificationController() {
    return new NotificationController();
  }
}
