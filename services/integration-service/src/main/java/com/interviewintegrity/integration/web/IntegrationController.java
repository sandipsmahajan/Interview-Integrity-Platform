package com.interviewintegrity.integration.web;

import com.interviewintegrity.integration.domain.IntegrationStatus;
import com.interviewintegrity.integration.service.IntegrationMapper;
import com.interviewintegrity.integration.service.IntegrationService;
import com.interviewintegrity.integration.web.dto.CreateIntegrationRequest;
import com.interviewintegrity.integration.web.dto.IntegrationResponse;
import com.interviewintegrity.integration.web.dto.UpdateIntegrationRequest;
import com.interviewintegrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Integration endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/integrations")
@Tag(name = "Integrations", description = "Manage external integrations")
public final class IntegrationController {

  private final IntegrationService integrationService;
  private final IntegrationMapper mapper;

  /** Creates the controller bound to the integration service and mapper. */
  public IntegrationController(IntegrationService integrationService, IntegrationMapper mapper) {
    this.integrationService = integrationService;
    this.mapper = mapper;
  }

  /** Creates an integration. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create an integration")
  public Mono<IntegrationResponse> create(
      Authentication authentication, @Valid @RequestBody CreateIntegrationRequest request) {
    return integrationService
        .createIntegration(
            SecurityPrincipals.organizationId(authentication),
            request.provider().trim(),
            request.name().trim(),
            request.credentialsRef().trim(),
            request.config(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Lists the integrations of the organization, optionally filtered by status. */
  @GetMapping
  @Operation(summary = "List integrations")
  public Flux<IntegrationResponse> list(
      Authentication authentication, @RequestParam(required = false) IntegrationStatus status) {
    return integrationService
        .listIntegrations(SecurityPrincipals.organizationId(authentication), status)
        .map(mapper::toResponse);
  }

  /** Returns a single integration. */
  @GetMapping("/{integrationId}")
  @Operation(summary = "Get an integration")
  public Mono<IntegrationResponse> get(
      Authentication authentication, @PathVariable UUID integrationId) {
    return integrationService
        .getIntegration(integrationId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Updates an integration. */
  @PutMapping("/{integrationId}")
  @Operation(summary = "Update an integration")
  public Mono<IntegrationResponse> update(
      Authentication authentication,
      @PathVariable UUID integrationId,
      @Valid @RequestBody UpdateIntegrationRequest request) {
    return integrationService
        .updateIntegration(
            integrationId,
            SecurityPrincipals.organizationId(authentication),
            request.name().trim(),
            request.config(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Connects an integration. */
  @PostMapping("/{integrationId}/connect")
  @Operation(summary = "Connect an integration")
  public Mono<IntegrationResponse> connect(
      Authentication authentication, @PathVariable UUID integrationId) {
    return integrationService
        .connectIntegration(
            integrationId,
            SecurityPrincipals.organizationId(authentication),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Disconnects an integration. */
  @PostMapping("/{integrationId}/disconnect")
  @Operation(summary = "Disconnect an integration")
  public Mono<IntegrationResponse> disconnect(
      Authentication authentication, @PathVariable UUID integrationId) {
    return integrationService
        .disconnectIntegration(
            integrationId,
            SecurityPrincipals.organizationId(authentication),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Marks an integration as errored. */
  @PatchMapping("/{integrationId}/error")
  @Operation(summary = "Mark an integration as errored")
  public Mono<IntegrationResponse> markError(
      Authentication authentication, @PathVariable UUID integrationId) {
    return integrationService
        .markIntegrationError(
            integrationId,
            SecurityPrincipals.organizationId(authentication),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Soft deletes an integration. */
  @DeleteMapping("/{integrationId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete an integration")
  public Mono<Void> delete(Authentication authentication, @PathVariable UUID integrationId) {
    return integrationService.deleteIntegration(
        integrationId,
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication));
  }
}
