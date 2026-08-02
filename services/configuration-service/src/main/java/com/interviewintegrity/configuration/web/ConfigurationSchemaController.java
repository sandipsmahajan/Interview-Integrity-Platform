package com.interviewintegrity.configuration.web;

import com.interviewintegrity.configuration.service.ConfigurationMapper;
import com.interviewintegrity.configuration.service.ConfigurationSchemaService;
import com.interviewintegrity.configuration.web.dto.ConfigurationSchemaResponse;
import com.interviewintegrity.configuration.web.dto.CreateConfigurationSchemaRequest;
import com.interviewintegrity.configuration.web.dto.UpdateConfigurationSchemaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Configuration schema catalog endpoints. */
@RestController
@RequestMapping("/api/v1/configuration-schema")
@Tag(name = "Configuration Schema", description = "Global configuration key catalog")
public final class ConfigurationSchemaController {

  private final ConfigurationSchemaService schemaService;
  private final ConfigurationMapper mapper;

  /** Creates the controller bound to the schema service and mapper. */
  public ConfigurationSchemaController(
      ConfigurationSchemaService schemaService, ConfigurationMapper mapper) {
    this.schemaService = schemaService;
    this.mapper = mapper;
  }

  /** Declares a new configuration key. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Declare a configuration key")
  public Mono<ConfigurationSchemaResponse> create(
      Authentication authentication, @Valid @RequestBody CreateConfigurationSchemaRequest request) {
    return schemaService
        .create(
            request.key().trim(),
            request.valueType(),
            request.defaultValue(),
            request.constraints(),
            request.description())
        .map(mapper::toResponse);
  }

  /** Lists every declared configuration key. */
  @GetMapping
  @Operation(summary = "List configuration schema")
  public Flux<ConfigurationSchemaResponse> list(Authentication authentication) {
    return schemaService.list().map(mapper::toResponse);
  }

  /** Returns a single schema entry. */
  @GetMapping("/{schemaId}")
  @Operation(summary = "Get a configuration schema entry")
  public Mono<ConfigurationSchemaResponse> get(
      Authentication authentication, @PathVariable UUID schemaId) {
    return schemaService.get(schemaId).map(mapper::toResponse);
  }

  /** Updates a schema entry. */
  @PatchMapping("/{schemaId}")
  @Operation(summary = "Update a configuration schema entry")
  public Mono<ConfigurationSchemaResponse> update(
      Authentication authentication,
      @PathVariable UUID schemaId,
      @Valid @RequestBody UpdateConfigurationSchemaRequest request) {
    return schemaService
        .update(
            schemaId,
            request.valueType(),
            request.defaultValue(),
            request.constraints(),
            request.description())
        .map(mapper::toResponse);
  }
}
