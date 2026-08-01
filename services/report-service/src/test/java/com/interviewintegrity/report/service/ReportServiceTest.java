package com.interviewintegrity.report.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.report.domain.Report;
import com.interviewintegrity.report.domain.ReportFormat;
import com.interviewintegrity.report.domain.ReportStatus;
import com.interviewintegrity.report.domain.ReportType;
import com.interviewintegrity.report.repository.ReportRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the report lifecycle service. */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

  @Mock private ReportRepository reportRepository;
  @Mock private ReportEventPublisher eventPublisher;

  private ReportService reportService;

  @BeforeEach
  void setUp() {
    reportService = new ReportService(reportRepository, eventPublisher);
  }

  @Test
  void createReportSavesInRequestedState() {
    UUID organizationId = UUID.randomUUID();
    UUID requestedBy = UUID.randomUUID();
    when(reportRepository.save(any(Report.class)))
        .thenAnswer(
            invocation -> {
              Report report = invocation.getArgument(0);
              report.setId(UUID.randomUUID());
              return Mono.just(report);
            });

    StepVerifier.create(
            reportService.createReport(
                organizationId,
                ReportType.INTERVIEW,
                "Interview report",
                ReportFormat.PDF,
                null,
                requestedBy))
        .assertNext(
            report -> {
              org.assertj.core.api.Assertions.assertThat(report.getStatus())
                  .isEqualTo(ReportStatus.REQUESTED);
              org.assertj.core.api.Assertions.assertThat(report.getOrganizationId())
                  .isEqualTo(organizationId);
              org.assertj.core.api.Assertions.assertThat(report.getFormat())
                  .isEqualTo(ReportFormat.PDF);
            })
        .verifyComplete();
  }

  @Test
  void generateReportCompletesAndPublishesEvent() {
    UUID organizationId = UUID.randomUUID();
    Report report =
        new Report(
            organizationId, ReportType.ORGANIZATION, "Org report", ReportFormat.CSV, null, null);
    report.setId(UUID.randomUUID());

    when(reportRepository.findByIdAndOrganization(report.getId(), organizationId))
        .thenReturn(Mono.just(report));
    when(reportRepository.save(any(Report.class))).thenReturn(Mono.just(report));
    when(eventPublisher.publishReportGenerated(any(Report.class))).thenReturn(Mono.empty());

    StepVerifier.create(reportService.generateReport(report.getId(), organizationId))
        .assertNext(
            generated -> {
              org.assertj.core.api.Assertions.assertThat(generated.getStatus())
                  .isEqualTo(ReportStatus.READY);
              org.assertj.core.api.Assertions.assertThat(generated.getGeneratedAt()).isNotNull();
              org.assertj.core.api.Assertions.assertThat(generated.getExpiresAt()).isNotNull();
            })
        .verifyComplete();

    verify(eventPublisher).publishReportGenerated(any(Report.class));
  }

  @Test
  void generateReportFailsForUnknownReport() {
    UUID reportId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    when(reportRepository.findByIdAndOrganization(reportId, organizationId))
        .thenReturn(Mono.empty());

    StepVerifier.create(reportService.generateReport(reportId, organizationId))
        .expectError(NotFoundException.class)
        .verify();

    verify(eventPublisher, never()).publishReportGenerated(any());
  }

  @Test
  void getReportReturnsNotFoundForUnknownReport() {
    UUID reportId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    when(reportRepository.findByIdAndOrganization(reportId, organizationId))
        .thenReturn(Mono.empty());

    StepVerifier.create(reportService.getReport(reportId, organizationId))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void listReportsDelegatesToStatusFilterWhenRequested() {
    UUID organizationId = UUID.randomUUID();
    Report report =
        new Report(
            organizationId, ReportType.SESSION, "Session report", ReportFormat.JSON, null, null);
    when(reportRepository.listByOrganizationAndStatus(organizationId, ReportStatus.READY))
        .thenReturn(Flux.just(report));

    StepVerifier.create(reportService.listReports(organizationId, ReportStatus.READY, null))
        .expectNext(report)
        .verifyComplete();
  }

  @Test
  void expireReportMarksReportExpired() {
    UUID organizationId = UUID.randomUUID();
    Report report =
        new Report(
            organizationId, ReportType.INTEGRITY, "Integrity report", ReportFormat.PDF, null, null);
    report.setId(UUID.randomUUID());

    when(reportRepository.findByIdAndOrganization(report.getId(), organizationId))
        .thenReturn(Mono.just(report));
    when(reportRepository.save(any(Report.class))).thenReturn(Mono.just(report));

    StepVerifier.create(reportService.expireReport(report.getId(), organizationId))
        .assertNext(
            expired ->
                org.assertj.core.api.Assertions.assertThat(expired.getStatus())
                    .isEqualTo(ReportStatus.EXPIRED))
        .verifyComplete();
  }
}
