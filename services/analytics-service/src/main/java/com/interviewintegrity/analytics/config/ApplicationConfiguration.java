package com.interviewintegrity.analytics.config;

import com.interviewintegrity.analytics.repository.AnalyticsJobRunRepository;
import com.interviewintegrity.analytics.repository.CandidateSummaryRepository;
import com.interviewintegrity.analytics.repository.IntegritySummaryRepository;
import com.interviewintegrity.analytics.repository.InterviewSummaryRepository;
import com.interviewintegrity.analytics.repository.OrganizationSummaryRepository;
import com.interviewintegrity.analytics.repository.RecruiterSummaryRepository;
import com.interviewintegrity.analytics.service.AnalyticsJobRunService;
import com.interviewintegrity.analytics.service.AnalyticsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;

/**
 * Explicit bean wiring for the analytics service application services.
 *
 * <p>Services are plain classes instantiated here rather than discovered by component scanning,
 * keeping the dependency graph visible in one place.
 */
@Configuration
public class ApplicationConfiguration {

  /** Provides the database client backed organization summary repository. */
  @Bean
  public OrganizationSummaryRepository organizationSummaryRepository(
      DatabaseClient databaseClient) {
    return new OrganizationSummaryRepository(databaseClient);
  }

  /** Provides the database client backed recruiter summary repository. */
  @Bean
  public RecruiterSummaryRepository recruiterSummaryRepository(DatabaseClient databaseClient) {
    return new RecruiterSummaryRepository(databaseClient);
  }

  /** Provides the database client backed candidate summary repository. */
  @Bean
  public CandidateSummaryRepository candidateSummaryRepository(DatabaseClient databaseClient) {
    return new CandidateSummaryRepository(databaseClient);
  }

  /** Provides the database client backed interview summary repository. */
  @Bean
  public InterviewSummaryRepository interviewSummaryRepository(DatabaseClient databaseClient) {
    return new InterviewSummaryRepository(databaseClient);
  }

  /** Provides the database client backed integrity summary repository. */
  @Bean
  public IntegritySummaryRepository integritySummaryRepository(DatabaseClient databaseClient) {
    return new IntegritySummaryRepository(databaseClient);
  }

  /** Provides the analytics service. */
  @Bean
  public AnalyticsService analyticsService(
      OrganizationSummaryRepository organizationSummaryRepository,
      RecruiterSummaryRepository recruiterSummaryRepository,
      CandidateSummaryRepository candidateSummaryRepository,
      InterviewSummaryRepository interviewSummaryRepository,
      IntegritySummaryRepository integritySummaryRepository) {
    return new AnalyticsService(
        organizationSummaryRepository,
        recruiterSummaryRepository,
        candidateSummaryRepository,
        interviewSummaryRepository,
        integritySummaryRepository);
  }

  /** Provides the analytics job run service. */
  @Bean
  public AnalyticsJobRunService analyticsJobRunService(AnalyticsJobRunRepository jobRunRepository) {
    return new AnalyticsJobRunService(jobRunRepository);
  }
}
