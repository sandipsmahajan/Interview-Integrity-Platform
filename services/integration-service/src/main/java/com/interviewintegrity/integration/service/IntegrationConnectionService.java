package com.interviewintegrity.integration.service;

import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.integration.domain.IntegrationConnection;
import com.interviewintegrity.integration.repository.IntegrationConnectionRepository;
import com.interviewintegrity.integration.repository.IntegrationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages the external account connections of an organization. */
public class IntegrationConnectionService {

  private final IntegrationConnectionRepository connectionRepository;
  private final IntegrationRepository integrationRepository;

  /** Wires the service with its repositories. */
  public IntegrationConnectionService(
      IntegrationConnectionRepository connectionRepository,
      IntegrationRepository integrationRepository) {
    this.connectionRepository = connectionRepository;
    this.integrationRepository = integrationRepository;
  }

  /** Creates and connects an external account under an integration. */
  @Transactional
  public Mono<IntegrationConnection> createConnection(
      UUID organizationId, UUID integrationId, String externalAccountId, List<String> scopes) {
    return integrationRepository
        .findLiveByIdAndOrganization(integrationId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Integration not found")))
        .flatMap(
            ignored ->
                connectionRepository
                    .findByIntegrationAndExternalAccount(integrationId, externalAccountId)
                    .hasElement())
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(
                    new ConflictException(
                        "Connection already exists for external account " + externalAccountId));
              }
              IntegrationConnection connection =
                  new IntegrationConnection(organizationId, integrationId, externalAccountId);
              connection.connect(scopes);
              return connectionRepository.save(connection);
            });
  }

  /** Returns a single connection of the organization. */
  @Transactional(readOnly = true)
  public Mono<IntegrationConnection> getConnection(UUID connectionId, UUID organizationId) {
    return connectionRepository
        .findByIdAndOrganization(connectionId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Integration connection not found")));
  }

  /** Lists the connections of an integration. */
  @Transactional(readOnly = true)
  public Flux<IntegrationConnection> listByIntegration(UUID integrationId, UUID organizationId) {
    return integrationRepository
        .findLiveByIdAndOrganization(integrationId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Integration not found")))
        .flatMapMany(
            ignored -> connectionRepository.listByIntegration(integrationId, organizationId));
  }

  /** Connects an existing connection with the granted scopes. */
  @Transactional
  public Mono<IntegrationConnection> connectConnection(
      UUID connectionId, UUID organizationId, List<String> scopes) {
    return getConnection(connectionId, organizationId)
        .map(
            connection -> {
              connection.connect(scopes);
              return connection;
            })
        .flatMap(connectionRepository::save);
  }

  /** Marks a connection as disconnected. */
  @Transactional
  public Mono<IntegrationConnection> disconnectConnection(UUID connectionId, UUID organizationId) {
    return getConnection(connectionId, organizationId)
        .map(
            connection -> {
              connection.disconnect();
              return connection;
            })
        .flatMap(connectionRepository::save);
  }

  /** Marks a connection as errored. */
  @Transactional
  public Mono<IntegrationConnection> markConnectionError(UUID connectionId, UUID organizationId) {
    return getConnection(connectionId, organizationId)
        .map(
            connection -> {
              connection.markError();
              return connection;
            })
        .flatMap(connectionRepository::save);
  }

  /** Records the completion of a synchronization run on a connection. */
  @Transactional
  public Mono<IntegrationConnection> recordSync(UUID connectionId, UUID organizationId) {
    return getConnection(connectionId, organizationId)
        .map(
            connection -> {
              connection.recordSync();
              return connection;
            })
        .flatMap(connectionRepository::save);
  }
}
