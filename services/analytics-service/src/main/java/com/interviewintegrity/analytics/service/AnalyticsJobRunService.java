package com.interviewintegrity.analytics.service;

import com.interviewintegrity.analytics.domain.AnalyticsJobRun;
import com.interviewintegrity.analytics.repository.AnalyticsJobRunRepository;
import com.interviewintegrity.exception.NotFoundException;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Tracks the observability log of analytics aggregation runs. */
public class AnalyticsJobRunService {

  private final AnalyticsJobRunRepository jobRunRepository;

  /** Wires the service with its repository. */
  public AnalyticsJobRunService(AnalyticsJobRunRepository jobRunRepository) {
    this.jobRunRepository = jobRunRepository;
  }

  /** Records the start of a job run. */
  @Transactional
  public Mono<AnalyticsJobRun> startJob(String jobName) {
    return jobRunRepository.save(new AnalyticsJobRun(jobName));
  }

  /** Marks a job run as succeeded. */
  @Transactional
  public Mono<AnalyticsJobRun> succeedJob(Long jobRunId, long recordsProcessed) {
    return requireJobRun(jobRunId)
        .map(
            run -> {
              run.succeed(recordsProcessed);
              return run;
            })
        .flatMap(jobRunRepository::save);
  }

  /** Marks a job run as failed. */
  @Transactional
  public Mono<AnalyticsJobRun> failJob(Long jobRunId, String errorMessage) {
    return requireJobRun(jobRunId)
        .map(
            run -> {
              run.fail(errorMessage);
              return run;
            })
        .flatMap(jobRunRepository::save);
  }

  /** Lists the most recent job runs. */
  @Transactional(readOnly = true)
  public Flux<AnalyticsJobRun> listRecent(int limit) {
    return jobRunRepository.listRecent(limit);
  }

  private Mono<AnalyticsJobRun> requireJobRun(Long jobRunId) {
    return jobRunRepository
        .findById(jobRunId)
        .switchIfEmpty(Mono.error(new NotFoundException("Job run not found")));
  }
}
