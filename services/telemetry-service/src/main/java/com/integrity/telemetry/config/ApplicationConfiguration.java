package com.integrity.telemetry.config;

import com.integrity.telemetry.repository.TelemetryEventRepository;
import com.integrity.telemetry.repository.TelemetryEventTypeRepository;
import com.integrity.telemetry.repository.TelemetrySessionRepository;
import com.integrity.telemetry.repository.TelemetrySummaryRepository;
import com.integrity.telemetry.service.KafkaTelemetryViolationPublisher;
import com.integrity.telemetry.service.TelemetryEventConsumer;
import com.integrity.telemetry.service.TelemetryEventService;
import com.integrity.telemetry.service.TelemetryEventTypeService;
import com.integrity.telemetry.service.TelemetrySessionService;
import com.integrity.telemetry.service.TelemetryViolationPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.sender.KafkaSender;

/**
 * Explicit bean wiring for the telemetry service application services.
 *
 * <p>Services are plain classes instantiated here rather than discovered by component scanning,
 * keeping the dependency graph visible in one place.
 */
@Configuration
public class ApplicationConfiguration {

  /** Provides the session repository. */
  @Bean
  public TelemetrySessionRepository telemetrySessionRepository(DatabaseClient databaseClient) {
    return new TelemetrySessionRepository(databaseClient);
  }

  /** Provides the event type repository. */
  @Bean
  public TelemetryEventTypeRepository telemetryEventTypeRepository(DatabaseClient databaseClient) {
    return new TelemetryEventTypeRepository(databaseClient);
  }

  /** Provides the raw event repository. */
  @Bean
  public TelemetryEventRepository telemetryEventRepository(DatabaseClient databaseClient) {
    return new TelemetryEventRepository(databaseClient);
  }

  /** Provides the hourly summary repository. */
  @Bean
  public TelemetrySummaryRepository telemetrySummaryRepository(DatabaseClient databaseClient) {
    return new TelemetrySummaryRepository(databaseClient);
  }

  /** Provides the violation publisher for policy engine signals. */
  @Bean
  public TelemetryViolationPublisher telemetryViolationPublisher(
      KafkaSender<String, String> sender, Environment environment) {
    String serviceName = environment.getProperty("spring.application.name", "telemetry-service");
    return new KafkaTelemetryViolationPublisher(sender, serviceName);
  }

  /** Provides the session service. */
  @Bean
  public TelemetrySessionService telemetrySessionService(
      TelemetrySessionRepository sessionRepository, TelemetrySummaryRepository summaryRepository) {
    return new TelemetrySessionService(sessionRepository, summaryRepository);
  }

  /** Provides the event type service. */
  @Bean
  public TelemetryEventTypeService telemetryEventTypeService(
      TelemetryEventTypeRepository eventTypeRepository) {
    return new TelemetryEventTypeService(eventTypeRepository);
  }

  /** Provides the raw event service. */
  @Bean
  public TelemetryEventService telemetryEventService(
      TelemetryEventRepository eventRepository,
      TelemetrySessionService sessionService,
      TelemetryViolationPublisher violationPublisher) {
    return new TelemetryEventService(eventRepository, sessionService, violationPublisher);
  }

  /** Provides the topic consumer and starts its subscription on startup. */
  @Bean
  public TelemetryEventConsumer telemetryEventConsumer(
      KafkaReceiver<String, String> receiver,
      KafkaSender<String, String> sender,
      TelemetrySessionService sessionService,
      TelemetryEventService eventService) {
    TelemetryEventConsumer consumer =
        new TelemetryEventConsumer(receiver, sender, sessionService, eventService);
    consumer.start();
    return consumer;
  }
}
