package com.interviewintegrity.analytics.repository;

import com.interviewintegrity.analytics.domain.AnalyticsJobRun;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/** Reactive repository for {@link AnalyticsJobRun} entities. */
public interface AnalyticsJobRunRepository extends ReactiveCrudRepository<AnalyticsJobRun, Long> {

  /** Lists the most recent job runs, newest first. */
  @Query("SELECT * FROM analytics_job_runs ORDER BY started_at DESC LIMIT :limit")
  Flux<AnalyticsJobRun> listRecent(int limit);
}
