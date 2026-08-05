package com.integrity.report.service;

import com.integrity.report.domain.Report;
import reactor.core.publisher.Mono;

/** Publishes report domain events onto the platform event bus. */
public interface ReportEventPublisher {

  /**
   * Publishes the report generated event.
   *
   * @param report the generated report
   * @return completion signal of the publish attempt
   */
  Mono<Void> publishReportGenerated(Report report);
}
