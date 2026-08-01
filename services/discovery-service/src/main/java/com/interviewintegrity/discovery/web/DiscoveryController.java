package com.interviewintegrity.discovery.web;

import com.interviewintegrity.discovery.domain.ServiceInstance;
import com.interviewintegrity.discovery.service.DiscoveryService;
import com.interviewintegrity.discovery.web.dto.InstanceResponse;
import com.interviewintegrity.discovery.web.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Service registry endpoints. */
@RestController
@RequestMapping("/api/v1/registry")
@Tag(name = "Service Registry", description = "Register, heartbeat and look up service instances")
public final class DiscoveryController {

  private final DiscoveryService discoveryService;

  /** Creates the controller bound to the registry service. */
  public DiscoveryController(DiscoveryService discoveryService) {
    this.discoveryService = discoveryService;
  }

  /** Registers a service instance. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Register an instance")
  public Mono<InstanceResponse> register(@Valid @RequestBody RegisterRequest request) {
    ServiceInstance instance =
        new ServiceInstance(
            request.instanceId().trim(),
            request.serviceId().trim(),
            request.host().trim(),
            request.port(),
            request.healthUrl(),
            request.metadata() == null ? java.util.Map.of() : request.metadata(),
            Instant.now());
    return discoveryService.register(instance).map(InstanceResponse::from);
  }

  /** Records a heartbeat for an instance. */
  @PutMapping("/{instanceId}/heartbeat")
  @Operation(summary = "Heartbeat an instance")
  public Mono<InstanceResponse> heartbeat(@PathVariable String instanceId) {
    return discoveryService.heartbeat(instanceId).map(InstanceResponse::from);
  }

  /** Deregisters an instance. */
  @DeleteMapping("/{instanceId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Deregister an instance")
  public Mono<Void> deregister(@PathVariable String instanceId) {
    return discoveryService.deregister(instanceId);
  }

  /** Looks up the live instances of a service. */
  @GetMapping("/services/{serviceId}")
  @Operation(summary = "Look up service instances")
  public Mono<List<InstanceResponse>> lookup(@PathVariable String serviceId) {
    return discoveryService.lookup(serviceId).map(InstanceResponse::fromAll);
  }

  /** Lists every registered instance. */
  @GetMapping
  @Operation(summary = "List all instances")
  public Mono<List<InstanceResponse>> list() {
    return discoveryService.list().map(InstanceResponse::fromAll);
  }
}
