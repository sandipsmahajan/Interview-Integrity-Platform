package com.interviewintegrity.configuration.repository;

import com.interviewintegrity.configuration.domain.ConfigurationHistory;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/** Reactive repository for {@link ConfigurationHistory} entities. */
public interface ConfigurationHistoryRepository
    extends ReactiveCrudRepository<ConfigurationHistory, Long> {

  /** Lists the version history of a configuration, newest first. */
  @Query(
      "SELECT * FROM configuration_history WHERE configuration_id = :configurationId "
          + "ORDER BY changed_at DESC")
  Flux<ConfigurationHistory> listByConfigurationId(UUID configurationId);
}
