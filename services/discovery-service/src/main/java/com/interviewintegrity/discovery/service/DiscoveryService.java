package com.interviewintegrity.discovery.service;

import com.interviewintegrity.discovery.domain.ServiceInstance;
import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.exception.NotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Mono;

/**
 * In-memory service registry.
 *
 * <p>Instances register with a heartbeat and are evicted when the heartbeat expires. This is the
 * lightweight fallback registry used while no external discovery server is provisioned.
 */
public class DiscoveryService {

  private final Map<String, ServiceInstance> instances = new ConcurrentHashMap<>();
  private final Map<String, Instant> evictionAt = new ConcurrentHashMap<>();
  private final long heartbeatTimeoutSeconds;

  /** Wires the service with the heartbeat timeout. */
  public DiscoveryService(long heartbeatTimeoutSeconds) {
    this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
  }

  /** Registers a new instance, rejecting duplicate instance ids. */
  public Mono<ServiceInstance> register(ServiceInstance instance) {
    if (instances.putIfAbsent(instance.instanceId(), instance) != null) {
      return Mono.error(new ConflictException("Instance already registered"));
    }
    evictionAt.put(instance.instanceId(), heartbeatDeadline());
    return Mono.just(instance);
  }

  /** Records a heartbeat for an instance, extending its lifetime. */
  public Mono<ServiceInstance> heartbeat(String instanceId) {
    ServiceInstance instance = instances.get(instanceId);
    if (instance == null) {
      return Mono.error(new NotFoundException("Instance not registered"));
    }
    ServiceInstance refreshed =
        new ServiceInstance(
            instance.instanceId(),
            instance.serviceId(),
            instance.host(),
            instance.port(),
            instance.healthUrl(),
            instance.metadata(),
            Instant.now());
    instances.put(instanceId, refreshed);
    evictionAt.put(instanceId, heartbeatDeadline());
    return Mono.just(refreshed);
  }

  /** Removes an instance from the registry. */
  public Mono<Void> deregister(String instanceId) {
    instances.remove(instanceId);
    evictionAt.remove(instanceId);
    return Mono.empty();
  }

  /** Looks up the live instances of a service. */
  public Mono<List<ServiceInstance>> lookup(String serviceId) {
    evictExpired();
    List<ServiceInstance> matches = new ArrayList<>();
    for (ServiceInstance instance : instances.values()) {
      if (instance.serviceId().equals(serviceId)) {
        matches.add(instance);
      }
    }
    return Mono.just(matches);
  }

  /** Lists every live instance of the registry. */
  public Mono<List<ServiceInstance>> list() {
    evictExpired();
    return Mono.just(new ArrayList<>(instances.values()));
  }

  private void evictExpired() {
    Instant now = Instant.now();
    instances.entrySet().removeIf(entry -> evictionDeadlinePassed(entry.getKey(), now));
  }

  private boolean evictionDeadlinePassed(String instanceId, Instant now) {
    Instant deadline = evictionAt.get(instanceId);
    return deadline != null && !deadline.isAfter(now);
  }

  private Instant heartbeatDeadline() {
    return Instant.now().plusSeconds(heartbeatTimeoutSeconds);
  }
}
