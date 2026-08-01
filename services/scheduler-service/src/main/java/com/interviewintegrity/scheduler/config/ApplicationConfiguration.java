package com.interviewintegrity.scheduler.config;

import com.interviewintegrity.scheduler.repository.JobExecutionRepository;
import com.interviewintegrity.scheduler.repository.JobLockRepository;
import com.interviewintegrity.scheduler.repository.ScheduledJobRepository;
import com.interviewintegrity.scheduler.service.JobExecutionService;
import com.interviewintegrity.scheduler.service.JobLockService;
import com.interviewintegrity.scheduler.service.ScheduledJobService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;

/**
 * Explicit bean wiring for the scheduler service application services.
 *
 * <p>Services are plain classes instantiated here rather than discovered by component scanning,
 * keeping the dependency graph visible in one place.
 */
@Configuration
public class ApplicationConfiguration {

  /** Provides the scheduled job service. */
  @Bean
  public ScheduledJobService scheduledJobService(
      ScheduledJobRepository jobRepository, JobLockRepository lockRepository) {
    return new ScheduledJobService(jobRepository, lockRepository);
  }

  /** Provides the job execution service. */
  @Bean
  public JobExecutionService jobExecutionService(
      JobExecutionRepository executionRepository, ScheduledJobRepository jobRepository) {
    return new JobExecutionService(executionRepository, jobRepository);
  }

  /** Provides the job lock service. */
  @Bean
  public JobLockService jobLockService(JobLockRepository lockRepository) {
    return new JobLockService(lockRepository);
  }

  /** Provides the distributed lock repository. */
  @Bean
  public JobLockRepository jobLockRepository(DatabaseClient databaseClient) {
    return new JobLockRepository(databaseClient);
  }
}
