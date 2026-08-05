package com.integrity.integration.web;

import com.integrity.integration.service.IntegrationConnectionService;
import com.integrity.integration.service.IntegrationMapper;
import com.integrity.integration.web.dto.ConnectionResponse;
import com.integrity.integration.web.dto.CreateConnectionRequest;
import com.integrity.security.SecurityPrincipals;
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

/** Connection endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/integration-connections")
@Tag(name = "Integration Connections", description = "Manage external account connections")
public final class IntegrationConnectionController {

  private final IntegrationConnectionService connectionService;
  private final IntegrationMapper mapper;

  /** Creates the controller bound to the connection service and mapper. */
  public IntegrationConnectionController(
      IntegrationConnectionService connectionService, IntegrationMapper mapper) {
    this.connectionService = connectionService;
    this.mapper = mapper;
  }

  /** Creates and connects an external account. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create an integration connection")
  public Mono<ConnectionResponse> create(
      Authentication authentication, @Valid @RequestBody CreateConnectionRequest request) {
    return connectionService
        .createConnection(
            SecurityPrincipals.organizationId(authentication),
            request.integrationId(),
            request.externalAccountId().trim(),
            request.scopes())
        .map(mapper::toResponse);
  }

  /** Lists the connections of an integration. */
  @GetMapping
  @Operation(summary = "List integration connections")
  public Flux<ConnectionResponse> list(
      Authentication authentication, @RequestParam UUID integrationId) {
    return connectionService
        .listByIntegration(integrationId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Returns a single connection. */
  @GetMapping("/{connectionId}")
  @Operation(summary = "Get an integration connection")
  public Mono<ConnectionResponse> get(
      Authentication authentication, @PathVariable UUID connectionId) {
    return connectionService
        .getConnection(connectionId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Reconnects a connection with the granted scopes. */
  @PostMapping("/{connectionId}/connect")
  @Operation(summary = "Connect an integration connection")
  public Mono<ConnectionResponse> connect(
      Authentication authentication,
      @PathVariable UUID connectionId,
      @Valid @RequestBody CreateConnectionRequest request) {
    return connectionService
        .connectConnection(
            connectionId, SecurityPrincipals.organizationId(authentication), request.scopes())
        .map(mapper::toResponse);
  }

  /** Disconnects a connection. */
  @PostMapping("/{connectionId}/disconnect")
  @Operation(summary = "Disconnect an integration connection")
  public Mono<ConnectionResponse> disconnect(
      Authentication authentication, @PathVariable UUID connectionId) {
    return connectionService
        .disconnectConnection(connectionId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Marks a connection as errored. */
  @PostMapping("/{connectionId}/error")
  @Operation(summary = "Mark an integration connection as errored")
  public Mono<ConnectionResponse> markError(
      Authentication authentication, @PathVariable UUID connectionId) {
    return connectionService
        .markConnectionError(connectionId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Records the completion of a synchronization run on a connection. */
  @PostMapping("/{connectionId}/sync")
  @Operation(summary = "Record a synchronization on an integration connection")
  public Mono<ConnectionResponse> recordSync(
      Authentication authentication, @PathVariable UUID connectionId) {
    return connectionService
        .recordSync(connectionId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }
}
