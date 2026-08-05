package com.integrity.configuration.service;

import com.integrity.configuration.domain.ConfigScope;
import com.integrity.configuration.domain.Configuration;
import com.integrity.configuration.domain.ConfigurationHistory;
import com.integrity.configuration.repository.ConfigurationHistoryRepository;
import com.integrity.configuration.repository.ConfigurationRepository;
import com.integrity.exception.ConflictException;
import com.integrity.exception.NotFoundException;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages tenant scoped configuration values. */
public class ConfigurationService {

  private static final String CONFIGURATION_NOT_FOUND = "Configuration not found";

  private final ConfigurationRepository configurationRepository;
  private final ConfigurationHistoryRepository historyRepository;

  /** Wires the service with its repositories. */
  public ConfigurationService(
      ConfigurationRepository configurationRepository,
      ConfigurationHistoryRepository historyRepository) {
    this.configurationRepository = configurationRepository;
    this.historyRepository = historyRepository;
  }

  /** Creates a configuration value, rejecting duplicates for the scope and key. */
  @Transactional
  public Mono<Configuration> create(
      UUID organizationId,
      ConfigScope scope,
      String key,
      String value,
      String description,
      UUID createdBy) {
    return configurationRepository
        .existsByOrganizationScopeAndKey(organizationId, scope, key)
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(
                    new ConflictException("Configuration already exists for scope and key"));
              }
              return configurationRepository.save(
                  new Configuration(organizationId, scope, key, value, description, createdBy));
            });
  }

  /** Lists the live configurations visible to an organization, optionally filtered by scope. */
  @Transactional(readOnly = true)
  public Flux<Configuration> list(UUID organizationId, ConfigScope scope) {
    if (scope == null) {
      return configurationRepository.listLiveVisible(organizationId);
    }
    return configurationRepository.listLiveByScope(organizationId, scope);
  }

  /** Returns a single configuration of an organization. */
  @Transactional(readOnly = true)
  public Mono<Configuration> get(UUID configurationId, UUID organizationId) {
    return configurationRepository
        .findLiveById(configurationId)
        .switchIfEmpty(Mono.error(new NotFoundException(CONFIGURATION_NOT_FOUND)))
        .flatMap(configuration -> assertOrganization(configuration, organizationId));
  }

  /** Updates a configuration value. */
  @Transactional
  public Mono<Configuration> update(
      UUID configurationId, UUID organizationId, String value, String description, UUID byUser) {
    return configurationRepository
        .findLiveById(configurationId)
        .switchIfEmpty(Mono.error(new NotFoundException(CONFIGURATION_NOT_FOUND)))
        .flatMap(configuration -> assertOrganization(configuration, organizationId))
        .map(
            configuration -> {
              configuration.update(value, description, byUser);
              return configuration;
            })
        .flatMap(configurationRepository::save);
  }

  /** Soft deletes a configuration value. */
  @Transactional
  public Mono<Void> delete(UUID configurationId, UUID organizationId, UUID byUser) {
    return configurationRepository
        .findLiveById(configurationId)
        .switchIfEmpty(Mono.error(new NotFoundException(CONFIGURATION_NOT_FOUND)))
        .flatMap(configuration -> assertOrganization(configuration, organizationId))
        .map(
            configuration -> {
              configuration.delete(byUser);
              return configuration;
            })
        .flatMap(configurationRepository::save)
        .then();
  }

  /** Lists the version history of a configuration. */
  @Transactional(readOnly = true)
  public Flux<ConfigurationHistory> history(UUID configurationId, UUID organizationId) {
    return configurationRepository
        .findLiveById(configurationId)
        .switchIfEmpty(Mono.error(new NotFoundException(CONFIGURATION_NOT_FOUND)))
        .flatMap(configuration -> assertOrganization(configuration, organizationId))
        .thenMany(historyRepository.listByConfigurationId(configurationId));
  }

  private Mono<Configuration> assertOrganization(Configuration configuration, UUID organizationId) {
    if (!organizationId.equals(configuration.getOrganizationId())) {
      return Mono.error(new NotFoundException(CONFIGURATION_NOT_FOUND));
    }
    return Mono.just(configuration);
  }
}
