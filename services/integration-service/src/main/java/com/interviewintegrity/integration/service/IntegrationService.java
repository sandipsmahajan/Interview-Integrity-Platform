package com.interviewintegrity.integration.service;

import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.integration.domain.Integration;
import com.interviewintegrity.integration.domain.IntegrationStatus;
import com.interviewintegrity.integration.repository.IntegrationRepository;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages the integration definitions of an organization. */
public class IntegrationService {

  private final IntegrationRepository integrationRepository;

  /** Wires the service with its repository. */
  public IntegrationService(IntegrationRepository integrationRepository) {
    this.integrationRepository = integrationRepository;
  }

  /** Creates an integration, rejecting a duplicate provider per organization. */
  @Transactional
  public Mono<Integration> createIntegration(
      UUID organizationId,
      String provider,
      String name,
      String credentialsRef,
      String config,
      UUID createdBy) {
    return integrationRepository
        .findLiveByOrganizationAndProvider(organizationId, provider)
        .hasElement()
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(
                    new ConflictException("Integration already exists for provider " + provider));
              }
              return integrationRepository.save(
                  new Integration(
                      organizationId, provider, name, credentialsRef, config, createdBy));
            });
  }

  /** Returns a single live integration of the organization. */
  @Transactional(readOnly = true)
  public Mono<Integration> getIntegration(UUID integrationId, UUID organizationId) {
    return integrationRepository
        .findLiveByIdAndOrganization(integrationId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Integration not found")));
  }

  /** Lists the live integrations of the organization, optionally filtered by status. */
  @Transactional(readOnly = true)
  public Flux<Integration> listIntegrations(UUID organizationId, IntegrationStatus status) {
    if (status == null) {
      return integrationRepository.listLiveByOrganization(organizationId);
    }
    return integrationRepository.listLiveByOrganizationAndStatus(organizationId, status);
  }

  /** Updates the display name and configuration of an integration. */
  @Transactional
  public Mono<Integration> updateIntegration(
      UUID integrationId, UUID organizationId, String name, String config, UUID updatedBy) {
    return getIntegration(integrationId, organizationId)
        .map(
            integration -> {
              integration.update(name, config, updatedBy);
              return integration;
            })
        .flatMap(integrationRepository::save);
  }

  /** Marks an integration as connected. */
  @Transactional
  public Mono<Integration> connectIntegration(
      UUID integrationId, UUID organizationId, UUID updatedBy) {
    return getIntegration(integrationId, organizationId)
        .map(
            integration -> {
              integration.connect(updatedBy);
              return integration;
            })
        .flatMap(integrationRepository::save);
  }

  /** Marks an integration as disconnected. */
  @Transactional
  public Mono<Integration> disconnectIntegration(
      UUID integrationId, UUID organizationId, UUID updatedBy) {
    return getIntegration(integrationId, organizationId)
        .map(
            integration -> {
              integration.disconnect(updatedBy);
              return integration;
            })
        .flatMap(integrationRepository::save);
  }

  /** Marks an integration as errored. */
  @Transactional
  public Mono<Integration> markIntegrationError(
      UUID integrationId, UUID organizationId, UUID updatedBy) {
    return getIntegration(integrationId, organizationId)
        .map(
            integration -> {
              integration.markError(updatedBy);
              return integration;
            })
        .flatMap(integrationRepository::save);
  }

  /** Soft deletes an integration. */
  @Transactional
  public Mono<Void> deleteIntegration(UUID integrationId, UUID organizationId, UUID deletedBy) {
    return getIntegration(integrationId, organizationId)
        .flatMap(
            integration -> {
              integration.delete(deletedBy);
              return integrationRepository.save(integration).then();
            });
  }
}
