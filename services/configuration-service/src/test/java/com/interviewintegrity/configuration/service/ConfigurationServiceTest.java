package com.interviewintegrity.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.interviewintegrity.configuration.domain.ConfigScope;
import com.interviewintegrity.configuration.domain.Configuration;
import com.interviewintegrity.configuration.domain.ConfigurationHistory;
import com.interviewintegrity.configuration.repository.ConfigurationHistoryRepository;
import com.interviewintegrity.configuration.repository.ConfigurationRepository;
import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.exception.NotFoundException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the tenant configuration service. */
@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest {

  @Mock private ConfigurationRepository configurationRepository;
  @Mock private ConfigurationHistoryRepository historyRepository;

  private ConfigurationService configurationService;

  @BeforeEach
  void setUp() {
    configurationService = new ConfigurationService(configurationRepository, historyRepository);
  }

  @Test
  void createRejectsDuplicateScopeAndKey() {
    UUID organizationId = UUID.randomUUID();
    when(configurationRepository.existsByOrganizationScopeAndKey(
            organizationId, ConfigScope.ORGANIZATION, "interview.timeout.seconds"))
        .thenReturn(Mono.just(true));

    StepVerifier.create(
            configurationService.create(
                organizationId,
                ConfigScope.ORGANIZATION,
                "interview.timeout.seconds",
                "3600",
                "Interview timeout",
                UUID.randomUUID()))
        .expectError(ConflictException.class)
        .verify();
  }

  @Test
  void createSavesNewConfiguration() {
    UUID organizationId = UUID.randomUUID();
    when(configurationRepository.existsByOrganizationScopeAndKey(
            organizationId, ConfigScope.ORGANIZATION, "interview.timeout.seconds"))
        .thenReturn(Mono.just(false));
    when(configurationRepository.save(any(Configuration.class)))
        .thenAnswer(
            invocation -> {
              Configuration configuration = invocation.getArgument(0);
              configuration.setId(UUID.randomUUID());
              return Mono.just(configuration);
            });

    StepVerifier.create(
            configurationService.create(
                organizationId,
                ConfigScope.ORGANIZATION,
                "interview.timeout.seconds",
                "3600",
                "Interview timeout",
                UUID.randomUUID()))
        .assertNext(
            configuration -> {
              assertThat(configuration.getOrganizationId()).isEqualTo(organizationId);
              assertThat(configuration.getScope()).isEqualTo(ConfigScope.ORGANIZATION);
              assertThat(configuration.getValue()).isEqualTo("3600");
            })
        .verifyComplete();
  }

  @Test
  void updateRejectsCrossTenantAccess() {
    UUID organizationId = UUID.randomUUID();
    UUID configurationId = UUID.randomUUID();
    Configuration configuration = configuration(organizationId, configurationId);

    when(configurationRepository.findLiveById(configurationId))
        .thenReturn(Mono.just(configuration));

    StepVerifier.create(
            configurationService.update(
                configurationId, UUID.randomUUID(), "7200", "New value", UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void updateChangesValue() {
    UUID organizationId = UUID.randomUUID();
    UUID configurationId = UUID.randomUUID();
    Configuration configuration = configuration(organizationId, configurationId);

    when(configurationRepository.findLiveById(configurationId))
        .thenReturn(Mono.just(configuration));
    when(configurationRepository.save(any(Configuration.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            configurationService.update(
                configurationId, organizationId, "7200", "Extended timeout", UUID.randomUUID()))
        .assertNext(
            result -> {
              assertThat(result.getValue()).isEqualTo("7200");
              assertThat(result.getDescription()).isEqualTo("Extended timeout");
            })
        .verifyComplete();
  }

  @Test
  void deleteSoftDeletesConfiguration() {
    UUID organizationId = UUID.randomUUID();
    UUID configurationId = UUID.randomUUID();
    Configuration configuration = configuration(organizationId, configurationId);

    when(configurationRepository.findLiveById(configurationId))
        .thenReturn(Mono.just(configuration));
    when(configurationRepository.save(any(Configuration.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            configurationService.delete(configurationId, organizationId, UUID.randomUUID()))
        .verifyComplete();
  }

  @Test
  void historyListsVersionRecords() {
    UUID organizationId = UUID.randomUUID();
    UUID configurationId = UUID.randomUUID();
    Configuration configuration = configuration(organizationId, configurationId);
    ConfigurationHistory history =
        new ConfigurationHistory(
            configurationId,
            organizationId,
            "interview.timeout.seconds",
            "3600",
            "7200",
            UUID.randomUUID(),
            Instant.now(),
            2);

    when(configurationRepository.findLiveById(configurationId))
        .thenReturn(Mono.just(configuration));
    when(historyRepository.listByConfigurationId(configurationId)).thenReturn(Flux.just(history));

    StepVerifier.create(configurationService.history(configurationId, organizationId))
        .assertNext(
            result -> {
              assertThat(result.getNewValue()).isEqualTo("7200");
              assertThat(result.getVersion()).isEqualTo(2);
            })
        .verifyComplete();
  }

  @Test
  void historyRejectsUnknownConfiguration() {
    UUID configurationId = UUID.randomUUID();
    when(configurationRepository.findLiveById(configurationId)).thenReturn(Mono.empty());

    StepVerifier.create(configurationService.history(configurationId, UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void getReturnsNotFoundForUnknownId() {
    UUID configurationId = UUID.randomUUID();
    when(configurationRepository.findLiveById(configurationId)).thenReturn(Mono.empty());

    StepVerifier.create(configurationService.get(configurationId, UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  private static Configuration configuration(UUID organizationId, UUID configurationId) {
    Configuration configuration =
        new Configuration(
            organizationId,
            ConfigScope.ORGANIZATION,
            "interview.timeout.seconds",
            "3600",
            "Interview timeout",
            UUID.randomUUID());
    configuration.setId(configurationId);
    return configuration;
  }
}
