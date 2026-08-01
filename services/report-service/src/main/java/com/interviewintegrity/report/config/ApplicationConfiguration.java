package com.interviewintegrity.report.config;

import com.interviewintegrity.report.repository.ReportRepository;
import com.interviewintegrity.report.repository.ReportRequestRepository;
import com.interviewintegrity.report.repository.ReportScheduleRepository;
import com.interviewintegrity.report.repository.ReportSectionRepository;
import com.interviewintegrity.report.service.KafkaReportEventPublisher;
import com.interviewintegrity.report.service.ReportEventPublisher;
import com.interviewintegrity.report.service.ReportRequestService;
import com.interviewintegrity.report.service.ReportScheduleService;
import com.interviewintegrity.report.service.ReportSectionService;
import com.interviewintegrity.report.service.ReportService;
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
