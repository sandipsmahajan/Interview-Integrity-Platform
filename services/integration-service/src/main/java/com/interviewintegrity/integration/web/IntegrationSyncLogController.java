package com.interviewintegrity.integration.web;

import com.interviewintegrity.integration.service.IntegrationMapper;
import com.interviewintegrity.integration.service.IntegrationSyncLogService;
import com.interviewintegrity.integration.web.dto.FinishSyncRequest;
import com.interviewintegrity.integration.web.dto.StartSyncRequest;
import com.interviewintegrity.integration.web.dto.SyncLogResponse;
import com.interviewintegrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Synchronization log endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/integration-sync-logs")
@Tag(name = "Integration Sync Logs", description = "Track synchronization runs")
public final class IntegrationSyncLogController {

  private final IntegrationSyncLogService syncLogService;
  private final IntegrationMapper mapper;

  /** Creates the controller bound to the sync log service and mapper. */
  public IntegrationSyncLogController(
      IntegrationSyncLogService syncLogService, IntegrationMapper mapper) {
    this.syncLogService = syncLogService;
    this.mapper = mapper;
  }

  /** Starts a synchronization run for a connection. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Start an integration sync run")
  public Mono<SyncLogResponse> start(
      Authentication authentication, @Valid @RequestBody StartSyncRequest request) {
    return syncLogService
        .startSync(
            SecurityPrincipals.organizationId(authentication),
            request.connectionId(),
            request.direction())
        .map(mapper::toResponse);
  }

  /** Completes a synchronization run successfully. */
  @PostMapping("/{syncLogId}/complete")
  @Operation(summary = "Complete an integration sync run")
  public Mono<SyncLogResponse> complete(
      Authentication authentication,
      @PathVariable Long syncLogId,
      @Valid @RequestBody FinishSyncRequest request) {
    return syncLogService
        .completeSync(
            syncLogId,
            SecurityPrincipals.organizationId(authentication),
            request.recordsProcessed())
        .map(mapper::toResponse);
  }

  /** Fails a synchronization run with an error detail. */
  @PostMapping("/{syncLogId}/fail")
  @Operation(summary = "Fail an integration sync run")
  public Mono<SyncLogResponse> fail(
      Authentication authentication,
      @PathVariable Long syncLogId,
      @Valid @RequestBody FinishSyncRequest request) {
    return syncLogService
        .failSync(
            syncLogId, SecurityPrincipals.organizationId(authentication), request.errorMessage())
        .map(mapper::toResponse);
  }

  /** Lists the synchronization runs of a connection. */
  @GetMapping
  @Operation(summary = "List integration sync runs")
  public Flux<SyncLogResponse> list(
      Authentication authentication, @RequestParam UUID connectionId) {
    return syncLogService
        .listByConnection(connectionId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }
}
