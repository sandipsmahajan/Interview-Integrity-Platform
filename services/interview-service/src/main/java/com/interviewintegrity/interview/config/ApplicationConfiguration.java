package com.interviewintegrity.interview.config;

import com.interviewintegrity.interview.repository.InterviewCalendarEventRepository;
import com.interviewintegrity.interview.repository.InterviewFeedbackRepository;
import com.interviewintegrity.interview.repository.InterviewPanelRepository;
import com.interviewintegrity.interview.repository.InterviewRepository;
import com.interviewintegrity.interview.repository.InterviewSessionRepository;
import com.interviewintegrity.interview.repository.InterviewerRepository;
import com.interviewintegrity.interview.service.InterviewCalendarEventService;
import com.interviewintegrity.interview.service.InterviewEventPublisher;
import com.interviewintegrity.interview.service.InterviewFeedbackService;
import com.interviewintegrity.interview.service.InterviewPanelService;
import com.interviewintegrity.interview.service.InterviewService;
import com.interviewintegrity.interview.service.InterviewSessionService;
import com.interviewintegrity.interview.service.InterviewerService;
import com.interviewintegrity.interview.service.KafkaInterviewEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.kafka.sender.KafkaSender;

/**
 * Explicit bean wiring for the interview service application services.
 *
 * <p>Services are plain classes instantiated here rather than discovered by component scanning,
 * keeping the dependency graph visible in one place.
 */
@Configuration
public class ApplicationConfiguration {

  /** Provides the database client backed panel bridge repository. */
  @Bean
  public InterviewPanelRepository interviewPanelRepository(DatabaseClient databaseClient) {
    return new InterviewPanelRepository(databaseClient);
  }

  /** Provides the event publisher for interview lifecycle events. */
  @Bean
  public InterviewEventPublisher interviewEventPublisher(
      KafkaSender<String, String> sender, Environment environment) {
    String serviceName = environment.getProperty("spring.application.name", "interview-service");
    return new KafkaInterviewEventPublisher(sender, serviceName);
  }

  /** Provides the interview service. */
  @Bean
  public InterviewService interviewService(
      InterviewRepository interviewRepository, InterviewEventPublisher eventPublisher) {
    return new InterviewService(interviewRepository, eventPublisher);
  }

  /** Provides the interview session service. */
  @Bean
  public InterviewSessionService interviewSessionService(
      InterviewSessionRepository sessionRepository,
      InterviewRepository interviewRepository,
      InterviewEventPublisher eventPublisher) {
    return new InterviewSessionService(sessionRepository, interviewRepository, eventPublisher);
  }

  /** Provides the interviewer service. */
  @Bean
  public InterviewerService interviewerService(InterviewerRepository interviewerRepository) {
    return new InterviewerService(interviewerRepository);
  }

  /** Provides the panel service. */
  @Bean
  public InterviewPanelService interviewPanelService(
      InterviewPanelRepository panelRepository,
      InterviewRepository interviewRepository,
      InterviewerRepository interviewerRepository) {
    return new InterviewPanelService(panelRepository, interviewRepository, interviewerRepository);
  }

  /** Provides the feedback service. */
  @Bean
  public InterviewFeedbackService interviewFeedbackService(
      InterviewFeedbackRepository feedbackRepository,
      InterviewRepository interviewRepository,
      InterviewerRepository interviewerRepository) {
    return new InterviewFeedbackService(
        feedbackRepository, interviewRepository, interviewerRepository);
  }

  /** Provides the calendar event service. */
  @Bean
  public InterviewCalendarEventService interviewCalendarEventService(
      InterviewCalendarEventRepository calendarRepository,
      InterviewRepository interviewRepository) {
    return new InterviewCalendarEventService(calendarRepository, interviewRepository);
  }
}
