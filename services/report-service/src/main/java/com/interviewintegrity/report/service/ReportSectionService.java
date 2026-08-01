package com.interviewintegrity.report.service;

import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.report.domain.ReportSection;
import com.interviewintegrity.report.repository.ReportRepository;
import com.interviewintegrity.report.repository.ReportSectionRepository;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages the ordered sections of a generated report. */
public class ReportSectionService {

  private final ReportSectionRepository sectionRepository;
  private final ReportRepository reportRepository;

  /** Wires the service with its repositories. */
  public ReportSectionService(
      ReportSectionRepository sectionRepository, ReportRepository reportRepository) {
    this.sectionRepository = sectionRepository;
    this.reportRepository = reportRepository;
  }

  /** Adds a section to a report, rejecting sections for unknown reports. */
  @Transactional
  public Mono<ReportSection> addSection(
      UUID reportId,
      UUID organizationId,
      String sectionType,
      String title,
      String content,
      int orderIndex) {
    return reportRepository
        .findByIdAndOrganization(reportId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Report not found")))
        .flatMap(
            report ->
                sectionRepository.save(
                    new ReportSection(
                        organizationId, reportId, sectionType, title, content, orderIndex)));
  }

  /** Returns a single section of a report. */
  @Transactional(readOnly = true)
  public Mono<ReportSection> getSection(UUID sectionId, UUID organizationId) {
    return sectionRepository
        .findByIdAndOrganization(sectionId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Report section not found")));
  }

  /** Lists the sections of a report in display order. */
  @Transactional(readOnly = true)
  public Flux<ReportSection> listSections(UUID reportId, UUID organizationId) {
    return sectionRepository.listByReportAndOrganization(reportId, organizationId);
  }

  /** Updates the payload and ordering of a section. */
  @Transactional
  public Mono<ReportSection> updateSection(
      UUID sectionId, UUID organizationId, String title, String content, int orderIndex) {
    return getSection(sectionId, organizationId)
        .map(
            section -> {
              section.update(title, content, orderIndex);
              return section;
            })
        .flatMap(sectionRepository::save);
  }

  /** Removes a section from a report. */
  @Transactional
  public Mono<Void> removeSection(UUID sectionId, UUID organizationId) {
    return getSection(sectionId, organizationId).flatMap(sectionRepository::delete).then();
  }
}
