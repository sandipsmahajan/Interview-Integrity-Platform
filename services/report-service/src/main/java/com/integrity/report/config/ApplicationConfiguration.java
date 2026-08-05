package com.integrity.report.config;

import com.integrity.report.repository.ReportRepository;
import com.integrity.report.repository.ReportRequestRepository;
import com.integrity.report.repository.ReportScheduleRepository;
import com.integrity.report.repository.ReportSectionRepository;
import com.integrity.report.service.KafkaReportEventPublisher;
import com.integrity.report.service.ReportEventPublisher;
import com.integrity.report.service.ReportRequestService;
import com.integrity.report.service.ReportScheduleService;
import com.integrity.report.service.ReportSectionService;
import com.integrity.report.service.ReportService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import reactor.kafka.sender.KafkaSender;

/**
 * Explicit bean wiring for the report service application services.
 *
 * <p>Services are plain classes instantiated here rather than discovered by component scanning,
 * keeping the dependency graph visible in one place.
 */
@Configuration
public class ApplicationConfiguration {

  /** Provides the event publisher for report lifecycle events. */
  @Bean
  public ReportEventPublisher reportEventPublisher(
      KafkaSender<String, String> sender, Environment environment) {
    String serviceName = environment.getProperty("spring.application.name", "report-service");
    return new KafkaReportEventPublisher(sender, serviceName);
  }

  /** Provides the report service. */
  @Bean
  public ReportService reportService(
      ReportRepository reportRepository, ReportEventPublisher eventPublisher) {
    return new ReportService(reportRepository, eventPublisher);
  }

  /** Provides the report section service. */
  @Bean
  public ReportSectionService reportSectionService(
      ReportSectionRepository sectionRepository, ReportRepository reportRepository) {
    return new ReportSectionService(sectionRepository, reportRepository);
  }

  /** Provides the report request service. */
  @Bean
  public ReportRequestService reportRequestService(
      ReportRequestRepository requestRepository, ReportRepository reportRepository) {
    return new ReportRequestService(requestRepository, reportRepository);
  }

  /** Provides the report schedule service. */
  @Bean
  public ReportScheduleService reportScheduleService(ReportScheduleRepository scheduleRepository) {
    return new ReportScheduleService(scheduleRepository);
  }
}
