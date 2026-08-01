package com.interviewintegrity.discovery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.interviewintegrity.discovery.domain.ServiceInstance;
import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class DiscoveryServiceTest {

  private DiscoveryService discoveryService;

  @BeforeEach
  void setUp() {
    discoveryService = new DiscoveryService(60);
  }

  private ServiceInstance instance(String id, String serviceId) {
    return new ServiceInstance(
        id,
        serviceId,
        "localhost",
        8081,
        "http://localhost:8081/actuator/health",
        Map.of(),
        Instant.now());
  }

  @Test
  void registerAndLookupByService() {
    discoveryService.register(instance("i-1", "identity-service")).block();
    discoveryService.register(instance("i-2", "identity-service")).block();
    discoveryService.register(instance("i-3", "organization-service")).block();

    List<ServiceInstance> matches = discoveryService.lookup("identity-service").block();
    assertThat(matches).hasSize(2);
  }

  @Test
  void duplicateInstanceIdIsRejected() {
    discoveryService.register(instance("i-1", "identity-service")).block();
    assertThrows(
        ConflictException.class,
        () -> discoveryService.register(instance("i-1", "identity-service")).block());
  }

  @Test
  void heartbeatExtendsLifetime() {
    discoveryService.register(instance("i-1", "identity-service")).block();
    ServiceInstance refreshed = discoveryService.heartbeat("i-1").block();
    assertThat(refreshed.serviceId()).isEqualTo("identity-service");
  }

  @Test
  void heartbeatOnUnknownInstanceFails() {
    assertThrows(NotFoundException.class, () -> discoveryService.heartbeat("missing").block());
  }

  @Test
  void deregisterRemovesInstance() {
    discoveryService.register(instance("i-1", "identity-service")).block();
    discoveryService.deregister("i-1").block();
    StepVerifier.create(discoveryService.lookup("identity-service"))
        .assertNext(list -> assertThat(list).isEmpty())
        .verifyComplete();
  }

  @Test
  void expiredInstancesAreEvicted() {
    DiscoveryService shortLived = new DiscoveryService(0);
    shortLived.register(instance("i-1", "identity-service")).block();
    StepVerifier.create(shortLived.list())
        .assertNext(list -> assertThat(list).isEmpty())
        .verifyComplete();
  }
}
