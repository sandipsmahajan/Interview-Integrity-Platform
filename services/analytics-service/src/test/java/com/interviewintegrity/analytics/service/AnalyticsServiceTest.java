package com.interviewintegrity.analytics.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interviewintegrity.analytics.domain.DailyOrganizationSummary;
import com.interviewintegrity.analytics.domain.DailyRecruiterSummary;
import com.interviewintegrity.analytics.repository.CandidateSummaryRepository;
import com.interviewintegrity.analytics.repository.IntegritySummaryRepository;
import com.interviewintegrity.analytics.repository.InterviewSummaryRepository;
import com.interviewintegrity.analytics.repository.OrganizationSummaryRepository;
import com.interviewintegrity.analytics.repository.RecruiterSummaryRepository;
import com.interviewintegrity.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the analytics summary service. */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

  @Mock private OrganizationSummaryRepository organizationSummaryRepository;
  @Mock private RecruiterSummaryRepository recruiterSummaryRepository;
  @Mock private CandidateSummaryRepository candidateSummaryRepository;
  @Mock private InterviewSummaryRepository interviewSummaryRepository;
  @Mock private IntegritySummaryRepository integritySummaryRepository;

  private AnalyticsService analyticsService;

  @BeforeEach
  void setUp() {
    analyticsService =
        new AnalyticsService(
            organizationSummaryRepository,
            recruiterSummaryRepository,
            candidateSummaryRepository,
            interviewSummaryRepository,
            integritySummaryRepository);
  }

  @Test
  void recordOrganizationUpsertsSummary() {
    UUID organizationId = UUID.randomUUID();
    DailyOrganizationSummary summary =
        new DailyOrganizationSummary(organizationId, LocalDate.now(ZoneOffset.UTC));
    when(organizationSummaryRepository.upsert(any(DailyOrganizationSummary.class)))
        .thenReturn(Mono.just(summary));

    StepVerifier.create(analyticsService.recordOrganization(summary))
        .expectNext(summary)
        .verifyComplete();

    verify(organizationSummaryRepository).upsert(summary);
  }

  @Test
  void getOrganizationReturnsNotFoundWhenAbsent() {
    UUID organizationId = UUID.randomUUID();
    LocalDate date = LocalDate.now(ZoneOffset.UTC);
    when(organizationSummaryRepository.find(organizationId, date)).thenReturn(Mono.empty());

    StepVerifier.create(analyticsService.getOrganization(organizationId, date))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void listOrganizationDelegatesToRepository() {
    UUID organizationId = UUID.randomUUID();
    LocalDate from = LocalDate.now(ZoneOffset.UTC).minusDays(6);
    LocalDate to = LocalDate.now(ZoneOffset.UTC);
    DailyOrganizationSummary summary = new DailyOrganizationSummary(organizationId, to);
    summary.update(10, 8, 0, 5, 3, 1, BigDecimal.valueOf(87.5));
    when(organizationSummaryRepository.list(organizationId, from, to))
        .thenReturn(Flux.just(summary));

    StepVerifier.create(analyticsService.listOrganization(organizationId, from, to))
        .expectNext(summary)
        .verifyComplete();
  }

  @Test
  void recordRecruiterUpsertsSummary() {
    UUID organizationId = UUID.randomUUID();
    DailyRecruiterSummary summary =
        new DailyRecruiterSummary(organizationId, UUID.randomUUID(), LocalDate.now(ZoneOffset.UTC));
    when(recruiterSummaryRepository.upsert(any(DailyRecruiterSummary.class)))
        .thenReturn(Mono.just(summary));

    StepVerifier.create(analyticsService.recordRecruiter(summary))
        .expectNext(summary)
        .verifyComplete();
  }

  @Test
  void refreshMonthlyViewsDelegatesToRepository() {
    when(organizationSummaryRepository.refreshMonthlyViews()).thenReturn(Mono.empty());

    StepVerifier.create(analyticsService.refreshMonthlyViews()).verifyComplete();

    verify(organizationSummaryRepository).refreshMonthlyViews();
  }
}
