package com.integrity.report.service;

import com.integrity.exception.NotFoundException;
import com.integrity.report.domain.ReportRequest;
import com.integrity.report.repository.ReportRepository;
import com.integrity.report.repository.ReportRequestRepository;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages the reproducible parameter records behind report generation. */
public class ReportRequestService {

  private final ReportRequestRepository requestRepository;
  private final ReportRepository reportRepository;

  /** Wires the service with its repositories. */
  public ReportRequestService(
      ReportRequestRepository requestRepository, ReportRepository reportRepository) {
    this.requestRepository = requestRepository;
    this.reportRepository = reportRepository;
  }

  /** Records the parameters used to generate a report. */
  @Transactional
  public Mono<ReportRequest> createRequest(
      UUID reportId,
      UUID organizationId,
      String aggregationLevel,
      String timeRange,
      String parameters,
      UUID requestedBy) {
    return reportRepository
        .findByIdAndOrganization(reportId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Report not found")))
        .flatMap(
            report ->
                requestRepository.save(
                    new ReportRequest(
                        organizationId,
                        reportId,
                        aggregationLevel,
                        timeRange,
                        parameters,
                        requestedBy)));
  }

  /** Returns a single request within the organization. */
  @Transactional(readOnly = true)
  public Mono<ReportRequest> getRequest(UUID requestId, UUID organizationId) {
    return requestRepository
        .findByIdAndOrganization(requestId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Report request not found")));
  }

  /** Lists the requests recorded for a report. */
  @Transactional(readOnly = true)
  public Flux<ReportRequest> listRequests(UUID reportId, UUID organizationId) {
    return requestRepository.listByReportAndOrganization(reportId, organizationId);
  }

  /** Marks a request as completed successfully. */
  @Transactional
  public Mono<ReportRequest> completeRequest(UUID requestId, UUID organizationId) {
    return getRequest(requestId, organizationId)
        .map(
            request -> {
              request.complete();
              return request;
            })
        .flatMap(requestRepository::save);
  }

  /** Marks a request as failed with the given error message. */
  @Transactional
  public Mono<ReportRequest> failRequest(UUID requestId, UUID organizationId, String errorMessage) {
    return getRequest(requestId, organizationId)
        .map(
            request -> {
              request.fail(errorMessage);
              return request;
            })
        .flatMap(requestRepository::save);
  }
}
