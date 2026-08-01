package com.interviewintegrity.report.service;

import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.report.domain.Report;
import com.interviewintegrity.report.domain.ReportFormat;
import com.interviewintegrity.report.domain.ReportStatus;
import com.interviewintegrity.report.domain.ReportType;
import com.interviewintegrity.report.repository.ReportRepository;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages report artifacts and their generation lifecycle. */
public class ReportService {

  private final ReportRepository reportRepository;
  private final ReportEventPublisher eventPublisher;

  /** Wires the service with its repository and event publisher. */
  public ReportService(ReportRepository reportRepository, ReportEventPublisher eventPublisher) {
    this.reportRepository = reportRepository;
    this.eventPublisher = eventPublisher;
  }

  /** Creates a report in the requested state. */
  @Transactional
  public Mono<Report> createReport(
      UUID organizationId,
      ReportType type,
      String title,
      ReportFormat format,
      String filters,
      UUID requestedBy) {
    return reportRepository.save(
        new Report(organizationId, type, title, format, filters, requestedBy));
  }

  /** Returns a single report within the organization. */
  @Transactional(readOnly = true)
  public Mono<Report> getReport(UUID reportId, UUID organizationId) {
    return reportRepository
        .findByIdAndOrganization(reportId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Report not found")));
  }

  /** Lists the reports of an organization, optionally filtered by status and type. */
  @Transactional(readOnly = true)
  public Flux<Report> listReports(UUID organizationId, ReportStatus status, ReportType type) {
    if (status != null) {
      return reportRepository.listByOrganizationAndStatus(organizationId, status);
    }
    if (type != null) {
      return reportRepository.listByOrganizationAndType(organizationId, type);
    }
    return reportRepository.listByOrganization(organizationId);
  }

  /** Marks a report as generated and publishes the report-generated event. */
  @Transactional
  public Mono<Report> generateReport(UUID reportId, UUID organizationId) {
    return requireReport(reportId, organizationId)
        .flatMap(
            report -> {
              report.regenerate();
              report.complete();
              return reportRepository.save(report);
            })
        .flatMap(report -> eventPublisher.publishReportGenerated(report).thenReturn(report));
  }

  /** Resets a report so it can be regenerated. */
  @Transactional
  public Mono<Report> regenerateReport(UUID reportId, UUID organizationId) {
    return requireReport(reportId, organizationId)
        .flatMap(
            report -> {
              report.regenerate();
              return reportRepository.save(report);
            });
  }

  /** Marks a report generation as failed. */
  @Transactional
  public Mono<Report> failReport(UUID reportId, UUID organizationId) {
    return requireReport(reportId, organizationId)
        .flatMap(
            report -> {
              report.fail();
              return reportRepository.save(report);
            });
  }

  /** Expires a report so it is no longer downloadable. */
  @Transactional
  public Mono<Report> expireReport(UUID reportId, UUID organizationId) {
    return requireReport(reportId, organizationId)
        .flatMap(
            report -> {
              report.expire();
              return reportRepository.save(report);
            });
  }

  /** Attaches the generated artifact to a report. */
  @Transactional
  public Mono<Report> attachStorage(UUID reportId, UUID organizationId, UUID storageObjectId) {
    if (storageObjectId == null) {
      return Mono.error(new ConflictException("storageObjectId must not be null"));
    }
    return requireReport(reportId, organizationId)
        .flatMap(
            report -> {
              report.attachStorage(storageObjectId);
              return reportRepository.save(report);
            });
  }

  private Mono<Report> requireReport(UUID reportId, UUID organizationId) {
    return reportRepository
        .findByIdAndOrganization(reportId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Report not found")));
  }
}
