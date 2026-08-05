package com.integrity.configuration.repository;

import com.integrity.configuration.domain.ConfigurationSchema;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link ConfigurationSchema} entities. */
public interface ConfigurationSchemaRepository
    extends ReactiveCrudRepository<ConfigurationSchema, UUID> {

  /** Finds a schema entry by key. */
  @Query("SELECT * FROM configuration_schema WHERE key = :key")
  Mono<ConfigurationSchema> findByKey(String key);

  /** Lists every schema entry ordered by key. */
  @Query("SELECT * FROM configuration_schema ORDER BY key")
  Flux<ConfigurationSchema> listAllOrderedByKey();

  /** Resolves whether a schema key already exists. */
  @Query("SELECT EXISTS(SELECT 1 FROM configuration_schema WHERE key = :key)")
  Mono<Boolean> existsByKey(String key);
}
