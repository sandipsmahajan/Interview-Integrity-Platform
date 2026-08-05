package com.integrity.analytics.web;

import com.integrity.analytics.domain.DailyCandidateSummary;
import com.integrity.analytics.domain.DailyIntegritySummary;
import com.integrity.analytics.domain.DailyInterviewSummary;
import com.integrity.analytics.domain.DailyOrganizationSummary;
import com.integrity.analytics.domain.DailyRecruiterSummary;
import com.integrity.analytics.service.AnalyticsService;
import com.integrity.analytics.web.dto.CandidateSummaryResponse;
import com.integrity.analytics.web.dto.IntegritySummaryResponse;
import com.integrity.analytics.web.dto.InterviewSummaryResponse;
import com.integrity.analytics.web.dto.OrganizationSummaryResponse;
import com.integrity.analytics.web.dto.RecordCandidateSummaryRequest;
import com.integrity.analytics.web.dto.RecordIntegritySummaryRequest;
import com.integrity.analytics.web.dto.RecordInterviewSummaryRequest;
import com.integrity.analytics.web.dto.RecordOrganizationSummaryRequest;
import com.integrity.analytics.web.dto.RecordRecruiterSummaryRequest;
import com.integrity.analytics.web.dto.RecruiterSummaryResponse;
import com.integrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Pre-aggregated summary endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Query pre-aggregated daily summaries")
public final class AnalyticsController {

  private static final int DEFAULT_RANGE_DAYS = 7;

  private final AnalyticsService analyticsService;

  /** Creates the controller bound to the analytics service. */
  public AnalyticsController(AnalyticsService analyticsService) {
    this.analyticsService = analyticsService;
  }

  /** Records a daily organization summary. */
  @PostMapping("/organization")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record an organization summary")
  public Mono<OrganizationSummaryResponse> recordOrganization(
      Authentication authentication, @Valid @RequestBody RecordOrganizationSummaryRequest request) {
    UUID organizationId = SecurityPrincipals.organizationId(authentication);
    return analyticsService
        .recordOrganization(toOrganizationSummary(organizationId, request))
        .map(this::toOrganizationResponse);
  }

  /** Returns the organization summary for a date. */
  @GetMapping("/organization")
  @Operation(summary = "Get an organization summary")
  public Mono<OrganizationSummaryResponse> getOrganization(
      Authentication authentication, @RequestParam(required = false) LocalDate date) {
    return analyticsService
        .getOrganization(SecurityPrincipals.organizationId(authentication), dateOrToday(date))
        .map(this::toOrganizationResponse);
  }

  /** Lists the organization summaries within a date range. */
  @GetMapping("/organization/range")
  @Operation(summary = "List organization summaries")
  public Flux<OrganizationSummaryResponse> listOrganization(
      Authentication authentication,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    return analyticsService
        .listOrganization(
            SecurityPrincipals.organizationId(authentication), fromOrRecent(from), toOrToday(to))
        .map(this::toOrganizationResponse);
  }

  /** Records a daily recruiter summary. */
  @PostMapping("/recruiter")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record a recruiter summary")
  public Mono<RecruiterSummaryResponse> recordRecruiter(
      Authentication authentication, @Valid @RequestBody RecordRecruiterSummaryRequest request) {
    UUID organizationId = SecurityPrincipals.organizationId(authentication);
    return analyticsService
        .recordRecruiter(toRecruiterSummary(organizationId, request))
        .map(this::toRecruiterResponse);
  }

  /** Returns the recruiter summary for a date. */
  @GetMapping("/recruiter")
  @Operation(summary = "Get a recruiter summary")
  public Mono<RecruiterSummaryResponse> getRecruiter(
      Authentication authentication,
      @RequestParam UUID recruiterId,
      @RequestParam(required = false) LocalDate date) {
    return analyticsService
        .getRecruiter(
            SecurityPrincipals.organizationId(authentication), recruiterId, dateOrToday(date))
        .map(this::toRecruiterResponse);
  }

  /** Lists the recruiter summaries within a date range. */
  @GetMapping("/recruiter/range")
  @Operation(summary = "List recruiter summaries")
  public Flux<RecruiterSummaryResponse> listRecruiter(
      Authentication authentication,
      @RequestParam UUID recruiterId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    return analyticsService
        .listRecruiter(
            SecurityPrincipals.organizationId(authentication),
            recruiterId,
            fromOrRecent(from),
            toOrToday(to))
        .map(this::toRecruiterResponse);
  }

  /** Records a daily candidate summary. */
  @PostMapping("/candidate")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record a candidate summary")
  public Mono<CandidateSummaryResponse> recordCandidate(
      Authentication authentication, @Valid @RequestBody RecordCandidateSummaryRequest request) {
    UUID organizationId = SecurityPrincipals.organizationId(authentication);
    return analyticsService
        .recordCandidate(toCandidateSummary(organizationId, request))
        .map(this::toCandidateResponse);
  }

  /** Returns the candidate summary for a date. */
  @GetMapping("/candidate")
  @Operation(summary = "Get a candidate summary")
  public Mono<CandidateSummaryResponse> getCandidate(
      Authentication authentication,
      @RequestParam UUID candidateId,
      @RequestParam(required = false) LocalDate date) {
    return analyticsService
        .getCandidate(
            SecurityPrincipals.organizationId(authentication), candidateId, dateOrToday(date))
        .map(this::toCandidateResponse);
  }

  /** Lists the candidate summaries within a date range. */
  @GetMapping("/candidate/range")
  @Operation(summary = "List candidate summaries")
  public Flux<CandidateSummaryResponse> listCandidate(
      Authentication authentication,
      @RequestParam UUID candidateId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    return analyticsService
        .listCandidate(
            SecurityPrincipals.organizationId(authentication),
            candidateId,
            fromOrRecent(from),
            toOrToday(to))
        .map(this::toCandidateResponse);
  }

  /** Records a daily interview summary. */
  @PostMapping("/interview")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record an interview summary")
  public Mono<InterviewSummaryResponse> recordInterview(
      Authentication authentication, @Valid @RequestBody RecordInterviewSummaryRequest request) {
    UUID organizationId = SecurityPrincipals.organizationId(authentication);
    return analyticsService
        .recordInterview(toInterviewSummary(organizationId, request))
        .map(this::toInterviewResponse);
  }

  /** Returns the interview summary for a date. */
  @GetMapping("/interview")
  @Operation(summary = "Get an interview summary")
  public Mono<InterviewSummaryResponse> getInterview(
      Authentication authentication,
      @RequestParam UUID interviewId,
      @RequestParam(required = false) LocalDate date) {
    return analyticsService
        .getInterview(
            SecurityPrincipals.organizationId(authentication), interviewId, dateOrToday(date))
        .map(this::toInterviewResponse);
  }

  /** Lists the interview summaries within a date range. */
  @GetMapping("/interview/range")
  @Operation(summary = "List interview summaries")
  public Flux<InterviewSummaryResponse> listInterview(
      Authentication authentication,
      @RequestParam UUID interviewId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    return analyticsService
        .listInterview(
            SecurityPrincipals.organizationId(authentication),
            interviewId,
            fromOrRecent(from),
            toOrToday(to))
        .map(this::toInterviewResponse);
  }

  /** Records a daily integrity summary. */
  @PostMapping("/integrity")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record an integrity summary")
  public Mono<IntegritySummaryResponse> recordIntegrity(
      Authentication authentication, @Valid @RequestBody RecordIntegritySummaryRequest request) {
    UUID organizationId = SecurityPrincipals.organizationId(authentication);
    return analyticsService
        .recordIntegrity(toIntegritySummary(organizationId, request))
        .map(this::toIntegrityResponse);
  }

  /** Returns the integrity summary for a date. */
  @GetMapping("/integrity")
  @Operation(summary = "Get an integrity summary")
  public Mono<IntegritySummaryResponse> getIntegrity(
      Authentication authentication, @RequestParam(required = false) LocalDate date) {
    return analyticsService
        .getIntegrity(SecurityPrincipals.organizationId(authentication), dateOrToday(date))
        .map(this::toIntegrityResponse);
  }

  /** Lists the integrity summaries within a date range. */
  @GetMapping("/integrity/range")
  @Operation(summary = "List integrity summaries")
  public Flux<IntegritySummaryResponse> listIntegrity(
      Authentication authentication,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    return analyticsService
        .listIntegrity(
            SecurityPrincipals.organizationId(authentication), fromOrRecent(from), toOrToday(to))
        .map(this::toIntegrityResponse);
  }

  /** Refreshes the monthly rollup materialized views. */
  @PostMapping("/refresh-monthly")
  @Operation(summary = "Refresh monthly materialized views")
  public Mono<Void> refreshMonthly() {
    return analyticsService.refreshMonthlyViews();
  }

  private DailyOrganizationSummary toOrganizationSummary(
      UUID organizationId, RecordOrganizationSummaryRequest request) {
    DailyOrganizationSummary summary = new DailyOrganizationSummary(organizationId, request.date());
    summary.update(
        request.interviewsScheduled(),
        request.interviewsCompleted(),
        request.interviewsCancelled(),
        request.candidatesActive(),
        request.recruitersActive(),
        request.violations(),
        request.avgIntegrityScore());
    return summary;
  }

  private DailyRecruiterSummary toRecruiterSummary(
      UUID organizationId, RecordRecruiterSummaryRequest request) {
    DailyRecruiterSummary summary =
        new DailyRecruiterSummary(organizationId, request.recruiterId(), request.date());
    summary.update(
        request.interviewsHeld(),
        request.interviewsCompleted(),
        request.candidatesContacted(),
        request.avgFeedbackRating(),
        request.violations());
    return summary;
  }

  private DailyCandidateSummary toCandidateSummary(
      UUID organizationId, RecordCandidateSummaryRequest request) {
    DailyCandidateSummary summary =
        new DailyCandidateSummary(organizationId, request.candidateId(), request.date());
    summary.update(
        request.interviewsAttended(),
        request.avgScore(),
        request.assessmentsCompleted(),
        request.violations());
    return summary;
  }

  private DailyInterviewSummary toInterviewSummary(
      UUID organizationId, RecordInterviewSummaryRequest request) {
    DailyInterviewSummary summary =
        new DailyInterviewSummary(organizationId, request.interviewId(), request.date());
    summary.update(
        request.durationMinutes(),
        request.integrityScore(),
        request.violations(),
        request.status());
    return summary;
  }

  private DailyIntegritySummary toIntegritySummary(
      UUID organizationId, RecordIntegritySummaryRequest request) {
    DailyIntegritySummary summary = new DailyIntegritySummary(organizationId, request.date());
    summary.update(
        request.totalEvents(),
        request.violationsTotal(),
        request.violationsBySeverity(),
        request.violationsByRule(),
        request.sessionsStarted(),
        request.sessionsAbandoned(),
        request.avgHeartbeatCadenceSeconds());
    return summary;
  }

  private static LocalDate dateOrToday(LocalDate date) {
    return date == null ? LocalDate.now(ZoneOffset.UTC) : date;
  }

  private static LocalDate toOrToday(LocalDate to) {
    return to == null ? LocalDate.now(ZoneOffset.UTC) : to;
  }

  private static LocalDate fromOrRecent(LocalDate from) {
    return from == null ? LocalDate.now(ZoneOffset.UTC).minusDays(DEFAULT_RANGE_DAYS) : from;
  }

  private OrganizationSummaryResponse toOrganizationResponse(DailyOrganizationSummary summary) {
    return new OrganizationSummaryResponse(
        summary.getSummaryDate(),
        summary.getOrganizationId(),
        summary.getInterviewsScheduled(),
        summary.getInterviewsCompleted(),
        summary.getInterviewsCancelled(),
        summary.getCandidatesActive(),
        summary.getRecruitersActive(),
        summary.getViolations(),
        summary.getAvgIntegrityScore(),
        summary.getCreatedAt(),
        summary.getUpdatedAt());
  }

  private RecruiterSummaryResponse toRecruiterResponse(DailyRecruiterSummary summary) {
    return new RecruiterSummaryResponse(
        summary.getSummaryDate(),
        summary.getOrganizationId(),
        summary.getRecruiterId(),
        summary.getInterviewsHeld(),
        summary.getInterviewsCompleted(),
        summary.getCandidatesContacted(),
        summary.getAvgFeedbackRating(),
        summary.getViolations(),
        summary.getCreatedAt(),
        summary.getUpdatedAt());
  }

  private CandidateSummaryResponse toCandidateResponse(DailyCandidateSummary summary) {
    return new CandidateSummaryResponse(
        summary.getSummaryDate(),
        summary.getOrganizationId(),
        summary.getCandidateId(),
        summary.getInterviewsAttended(),
        summary.getAvgScore(),
        summary.getAssessmentsCompleted(),
        summary.getViolations(),
        summary.getCreatedAt(),
        summary.getUpdatedAt());
  }

  private InterviewSummaryResponse toInterviewResponse(DailyInterviewSummary summary) {
    return new InterviewSummaryResponse(
        summary.getSummaryDate(),
        summary.getOrganizationId(),
        summary.getInterviewId(),
        summary.getDurationMinutes(),
        summary.getIntegrityScore(),
        summary.getViolations(),
        summary.getStatus(),
        summary.getCreatedAt(),
        summary.getUpdatedAt());
  }

  private IntegritySummaryResponse toIntegrityResponse(DailyIntegritySummary summary) {
    return new IntegritySummaryResponse(
        summary.getSummaryDate(),
        summary.getOrganizationId(),
        summary.getTotalEvents(),
        summary.getViolationsTotal(),
        summary.getViolationsBySeverity(),
        summary.getViolationsByRule(),
        summary.getSessionsStarted(),
        summary.getSessionsAbandoned(),
        summary.getAvgHeartbeatCadenceSeconds(),
        summary.getCreatedAt(),
        summary.getUpdatedAt());
  }
}
