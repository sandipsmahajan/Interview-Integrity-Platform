package com.integrity.integration.service;

import com.integrity.exception.NotFoundException;
import com.integrity.integration.domain.IntegrationSyncLog;
import com.integrity.integration.domain.SyncDirection;
import com.integrity.integration.repository.IntegrationConnectionRepository;
import com.integrity.integration.repository.IntegrationSyncLogRepository;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Tracks the synchronization runs of the connections of an organization. */
public class IntegrationSyncLogService {

  private final IntegrationSyncLogRepository syncLogRepository;
  private final IntegrationConnectionRepository connectionRepository;

  /** Wires the service with its repositories. */
  public IntegrationSyncLogService(
      IntegrationSyncLogRepository syncLogRepository,
      IntegrationConnectionRepository connectionRepository) {
    this.syncLogRepository = syncLogRepository;
    this.connectionRepository = connectionRepository;
  }

  /** Starts a synchronization run for a connection. */
  @Transactional
  public Mono<IntegrationSyncLog> startSync(
      UUID organizationId, UUID connectionId, SyncDirection direction) {
    return connectionRepository
        .findByIdAndOrganization(connectionId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Integration connection not found")))
        .flatMap(
            ignored ->
                syncLogRepository.save(
                    new IntegrationSyncLog(organizationId, connectionId, direction)));
  }

  /** Completes a synchronization run successfully. */
  @Transactional
  public Mono<IntegrationSyncLog> completeSync(
      Long syncLogId, UUID organizationId, long recordsProcessed) {
    return getSyncLog(syncLogId, organizationId)
        .map(
            syncLog -> {
              syncLog.complete(recordsProcessed);
              return syncLog;
            })
        .flatMap(syncLogRepository::save);
  }

  /** Fails a synchronization run with an error detail. */
  @Transactional
  public Mono<IntegrationSyncLog> failSync(
      Long syncLogId, UUID organizationId, String errorMessage) {
    return getSyncLog(syncLogId, organizationId)
        .map(
            syncLog -> {
              syncLog.fail(errorMessage);
              return syncLog;
            })
        .flatMap(syncLogRepository::save);
  }

  /** Lists the synchronization runs of a connection. */
  @Transactional(readOnly = true)
  public Flux<IntegrationSyncLog> listByConnection(UUID connectionId, UUID organizationId) {
    return connectionRepository
        .findByIdAndOrganization(connectionId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Integration connection not found")))
        .flatMapMany(ignored -> syncLogRepository.listByConnection(connectionId));
  }

  private Mono<IntegrationSyncLog> getSyncLog(Long syncLogId, UUID organizationId) {
    return syncLogRepository
        .findByIdAndOrganization(syncLogId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Integration sync log not found")));
  }
}
