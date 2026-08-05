package com.integrity.configuration.web;

import com.integrity.configuration.domain.ConfigScope;
import com.integrity.configuration.service.ConfigurationMapper;
import com.integrity.configuration.service.ConfigurationService;
import com.integrity.configuration.web.dto.ConfigurationHistoryResponse;
import com.integrity.configuration.web.dto.ConfigurationResponse;
import com.integrity.configuration.web.dto.CreateConfigurationRequest;
import com.integrity.configuration.web.dto.UpdateConfigurationRequest;
import com.integrity.security.SecurityPrincipals;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Tenant scoped configuration endpoints. */
@RestController
@RequestMapping("/api/v1/configurations")
@Tag(name = "Configurations", description = "Tenant scoped configuration values")
public final class ConfigurationController {

  private final ConfigurationService configurationService;
  private final ConfigurationMapper mapper;

  /** Creates the controller bound to the configuration service and mapper. */
  public ConfigurationController(
      ConfigurationService configurationService, ConfigurationMapper mapper) {
    this.configurationService = configurationService;
    this.mapper = mapper;
  }

  /** Creates a configuration value. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a configuration")
  public Mono<ConfigurationResponse> create(
      Authentication authentication, @Valid @RequestBody CreateConfigurationRequest request) {
    return configurationService
        .create(
            SecurityPrincipals.organizationId(authentication),
            request.scope(),
            request.key().trim(),
            request.value(),
            request.description(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Lists the configurations visible to the organization, optionally filtered by scope. */
  @GetMapping
  @Operation(summary = "List configurations")
  public Flux<ConfigurationResponse> list(
      Authentication authentication, @RequestParam(required = false) ConfigScope scope) {
    return configurationService
        .list(SecurityPrincipals.organizationId(authentication), scope)
        .map(mapper::toResponse);
  }

  /** Returns a single configuration. */
  @GetMapping("/{configurationId}")
  @Operation(summary = "Get a configuration")
  public Mono<ConfigurationResponse> get(
      Authentication authentication, @PathVariable UUID configurationId) {
    return configurationService
        .get(configurationId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Returns the version history of a configuration. */
  @GetMapping("/{configurationId}/history")
  @Operation(summary = "Get configuration history")
  public Flux<ConfigurationHistoryResponse> history(
      Authentication authentication, @PathVariable UUID configurationId) {
    return configurationService
        .history(configurationId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toHistoryResponse);
  }

  /** Updates a configuration value. */
  @PatchMapping("/{configurationId}")
  @Operation(summary = "Update a configuration")
  public Mono<ConfigurationResponse> update(
      Authentication authentication,
      @PathVariable UUID configurationId,
      @Valid @RequestBody UpdateConfigurationRequest request) {
    return configurationService
        .update(
            configurationId,
            SecurityPrincipals.organizationId(authentication),
            request.value(),
            request.description(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Soft deletes a configuration value. */
  @DeleteMapping("/{configurationId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a configuration")
  public Mono<Void> delete(Authentication authentication, @PathVariable UUID configurationId) {
    return configurationService.delete(
        configurationId,
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication));
  }
}
