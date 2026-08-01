package com.interviewintegrity.analytics.service;

import com.interviewintegrity.analytics.domain.DailyCandidateSummary;
import com.interviewintegrity.analytics.domain.DailyIntegritySummary;
import com.interviewintegrity.analytics.domain.DailyInterviewSummary;
import com.interviewintegrity.analytics.domain.DailyOrganizationSummary;
import com.interviewintegrity.analytics.domain.DailyRecruiterSummary;
import com.interviewintegrity.analytics.repository.CandidateSummaryRepository;
import com.interviewintegrity.analytics.repository.IntegritySummaryRepository;
import com.interviewintegrity.analytics.repository.InterviewSummaryRepository;
import com.interviewintegrity.analytics.repository.OrganizationSummaryRepository;
import com.interviewintegrity.analytics.repository.RecruiterSummaryRepository;
import com.interviewintegrity.exception.NotFoundException;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Aggregates and queries the pre-aggregated daily summaries of an organization. */
public class AnalyticsService {

  private final OrganizationSummaryRepository organizationSummaryRepository;
  private final RecruiterSummaryRepository recruiterSummaryRepository;
  private final CandidateSummaryRepository candidateSummaryRepository;
  private final InterviewSummaryRepository interviewSummaryRepository;
  private final IntegritySummaryRepository integritySummaryRepository;

  /** Wires the service with its summary repositories. */
  public AnalyticsService(
      OrganizationSummaryRepository organizationSummaryRepository,
      RecruiterSummaryRepository recruiterSummaryRepository,
      CandidateSummaryRepository candidateSummaryRepository,
      InterviewSummaryRepository interviewSummaryRepository,
      IntegritySummaryRepository integritySummaryRepository) {
    this.organizationSummaryRepository = organizationSummaryRepository;
    this.recruiterSummaryRepository = recruiterSummaryRepository;
    this.candidateSummaryRepository = candidateSummaryRepository;
    this.interviewSummaryRepository = interviewSummaryRepository;
    this.integritySummaryRepository = integritySummaryRepository;
  }

  /** Records a daily organization summary. */
  @Transactional
  public Mono<DailyOrganizationSummary> recordOrganization(DailyOrganizationSummary summary) {
    return organizationSummaryRepository.upsert(summary);
  }

  /** Returns the organization summary for a date. */
  @Transactional(readOnly = true)
  public Mono<DailyOrganizationSummary> getOrganization(UUID organizationId, LocalDate date) {
    return organizationSummaryRepository
        .find(organizationId, date)
        .switchIfEmpty(
            Mono.error(new NotFoundException("Organization summary not found for date " + date)));
  }

  /** Lists the organization summaries within a date range. */
  @Transactional(readOnly = true)
  public Flux<DailyOrganizationSummary> listOrganization(
      UUID organizationId, LocalDate from, LocalDate to) {
    return organizationSummaryRepository.list(organizationId, from, to);
  }

  /** Records a daily recruiter summary. */
  @Transactional
  public Mono<DailyRecruiterSummary> recordRecruiter(DailyRecruiterSummary summary) {
    return recruiterSummaryRepository.upsert(summary);
  }

  /** Returns the recruiter summary for a date. */
  @Transactional(readOnly = true)
  public Mono<DailyRecruiterSummary> getRecruiter(
      UUID organizationId, UUID recruiterId, LocalDate date) {
    return recruiterSummaryRepository
        .find(organizationId, recruiterId, date)
        .switchIfEmpty(
            Mono.error(new NotFoundException("Recruiter summary not found for date " + date)));
  }

  /** Lists the recruiter summaries within a date range. */
  @Transactional(readOnly = true)
  public Flux<DailyRecruiterSummary> listRecruiter(
      UUID organizationId, UUID recruiterId, LocalDate from, LocalDate to) {
    return recruiterSummaryRepository.list(organizationId, recruiterId, from, to);
  }

  /** Records a daily candidate summary. */
  @Transactional
  public Mono<DailyCandidateSummary> recordCandidate(DailyCandidateSummary summary) {
    return candidateSummaryRepository.upsert(summary);
  }

  /** Returns the candidate summary for a date. */
  @Transactional(readOnly = true)
  public Mono<DailyCandidateSummary> getCandidate(
      UUID organizationId, UUID candidateId, LocalDate date) {
    return candidateSummaryRepository
        .find(organizationId, candidateId, date)
        .switchIfEmpty(
            Mono.error(new NotFoundException("Candidate summary not found for date " + date)));
  }

  /** Lists the candidate summaries within a date range. */
  @Transactional(readOnly = true)
  public Flux<DailyCandidateSummary> listCandidate(
      UUID organizationId, UUID candidateId, LocalDate from, LocalDate to) {
    return candidateSummaryRepository.list(organizationId, candidateId, from, to);
  }

  /** Records a daily interview summary. */
  @Transactional
  public Mono<DailyInterviewSummary> recordInterview(DailyInterviewSummary summary) {
    return interviewSummaryRepository.upsert(summary);
  }

  /** Returns the interview summary for a date. */
  @Transactional(readOnly = true)
  public Mono<DailyInterviewSummary> getInterview(
      UUID organizationId, UUID interviewId, LocalDate date) {
    return interviewSummaryRepository
        .find(organizationId, interviewId, date)
        .switchIfEmpty(
            Mono.error(new NotFoundException("Interview summary not found for date " + date)));
  }

  /** Lists the interview summaries within a date range. */
  @Transactional(readOnly = true)
  public Flux<DailyInterviewSummary> listInterview(
      UUID organizationId, UUID interviewId, LocalDate from, LocalDate to) {
    return interviewSummaryRepository.list(organizationId, interviewId, from, to);
  }

  /** Records a daily integrity summary. */
  @Transactional
  public Mono<DailyIntegritySummary> recordIntegrity(DailyIntegritySummary summary) {
    return integritySummaryRepository.upsert(summary);
  }

  /** Returns the integrity summary for a date. */
  @Transactional(readOnly = true)
  public Mono<DailyIntegritySummary> getIntegrity(UUID organizationId, LocalDate date) {
    return integritySummaryRepository
        .find(organizationId, date)
        .switchIfEmpty(
            Mono.error(new NotFoundException("Integrity summary not found for date " + date)));
  }

  /** Lists the integrity summaries within a date range. */
  @Transactional(readOnly = true)
  public Flux<DailyIntegritySummary> listIntegrity(
      UUID organizationId, LocalDate from, LocalDate to) {
    return integritySummaryRepository.list(organizationId, from, to);
  }

  /** Refreshes the monthly rollup materialized views. */
  @Transactional
  public Mono<Void> refreshMonthlyViews() {
    return organizationSummaryRepository.refreshMonthlyViews();
  }
}
