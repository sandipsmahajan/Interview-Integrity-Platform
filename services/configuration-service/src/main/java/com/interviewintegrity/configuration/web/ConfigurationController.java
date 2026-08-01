package com.interviewintegrity.configuration.web;

import com.interviewintegrity.configuration.domain.ConfigScope;
import com.interviewintegrity.configuration.domain.Configuration;
import com.interviewintegrity.configuration.domain.ConfigurationHistory;
import com.interviewintegrity.configuration.service.ConfigurationService;
import com.interviewintegrity.configuration.web.dto.ConfigurationHistoryResponse;
import com.interviewintegrity.configuration.web.dto.ConfigurationResponse;
import com.interviewintegrity.configuration.web.dto.CreateConfigurationRequest;
import com.interviewintegrity.configuration.web.dto.UpdateConfigurationRequest;
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

  /** Creates the controller bound to the configuration service. */
  public ConfigurationController(ConfigurationService configurationService) {
    this.configurationService = configurationService;
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
        .map(this::toResponse);
  }

  /** Lists the configurations visible to the organization, optionally filtered by scope. */
  @GetMapping
  @Operation(summary = "List configurations")
  public Flux<ConfigurationResponse> list(
      Authentication authentication, @RequestParam(required = false) ConfigScope scope) {
    return configurationService
        .list(SecurityPrincipals.organizationId(authentication), scope)
        .map(this::toResponse);
  }

  /** Returns a single configuration. */
  @GetMapping("/{configurationId}")
  @Operation(summary = "Get a configuration")
  public Mono<ConfigurationResponse> get(
      Authentication authentication, @PathVariable UUID configurationId) {
    return configurationService
        .get(configurationId, SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  /** Returns the version history of a configuration. */
  @GetMapping("/{configurationId}/history")
  @Operation(summary = "Get configuration history")
  public Flux<ConfigurationHistoryResponse> history(
      Authentication authentication, @PathVariable UUID configurationId) {
    return configurationService
        .history(configurationId, SecurityPrincipals.organizationId(authentication))
        .map(this::toHistoryResponse);
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
        .map(this::toResponse);
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

  private ConfigurationResponse toResponse(Configuration configuration) {
    return new ConfigurationResponse(
        configuration.getId(),
        configuration.getOrganizationId(),
        configuration.getScope(),
        configuration.getKey(),
        configuration.getValue(),
        configuration.getDescription(),
        configuration.getCreatedAt(),
        configuration.getUpdatedAt());
  }

  private ConfigurationHistoryResponse toHistoryResponse(ConfigurationHistory history) {
    return new ConfigurationHistoryResponse(
        history.getId(),
        history.getConfigurationId(),
        history.getOrganizationId(),
        history.getKey(),
        history.getOldValue(),
        history.getNewValue(),
        history.getChangedBy(),
        history.getChangedAt(),
        history.getVersion());
  }
}
