package com.integrity.configuration.service;

import com.integrity.configuration.domain.ConfigValueType;
import com.integrity.configuration.domain.ConfigurationSchema;
import com.integrity.configuration.repository.ConfigurationSchemaRepository;
import com.integrity.exception.ConflictException;
import com.integrity.exception.NotFoundException;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages the global configuration schema catalog. */
public class ConfigurationSchemaService {

  private final ConfigurationSchemaRepository schemaRepository;

  /** Wires the service with its repository. */
  public ConfigurationSchemaService(ConfigurationSchemaRepository schemaRepository) {
    this.schemaRepository = schemaRepository;
  }

  /** Declares a new configuration key, rejecting duplicate keys. */
  @Transactional
  public Mono<ConfigurationSchema> create(
      String key,
      ConfigValueType valueType,
      String defaultValue,
      String constraints,
      String description) {
    return schemaRepository
        .existsByKey(key)
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(new ConflictException("Schema key already exists"));
              }
              return schemaRepository.save(
                  new ConfigurationSchema(key, valueType, defaultValue, constraints, description));
            });
  }

  /** Lists every declared configuration key. */
  @Transactional(readOnly = true)
  public Flux<ConfigurationSchema> list() {
    return schemaRepository.listAllOrderedByKey();
  }

  /** Returns a single schema entry. */
  @Transactional(readOnly = true)
  public Mono<ConfigurationSchema> get(UUID schemaId) {
    return schemaRepository
        .findById(schemaId)
        .switchIfEmpty(Mono.error(new NotFoundException("Configuration schema not found")));
  }

  /** Updates a schema entry. */
  @Transactional
  public Mono<ConfigurationSchema> update(
      UUID schemaId,
      ConfigValueType valueType,
      String defaultValue,
      String constraints,
      String description) {
    return schemaRepository
        .findById(schemaId)
        .switchIfEmpty(Mono.error(new NotFoundException("Configuration schema not found")))
        .map(
            schema -> {
              schema.update(valueType, defaultValue, constraints, description);
              return schema;
            })
        .flatMap(schemaRepository::save);
  }
}
