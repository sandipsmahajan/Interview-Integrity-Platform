package com.interviewintegrity.discovery.web.dto;

import com.interviewintegrity.discovery.domain.ServiceInstance;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Public representation of a service instance.
 *
 * @param instanceId unique instance identifier
 * @param serviceId logical service name
 * @param host host the instance runs on
 * @param port port the instance listens on
 * @param healthUrl optional health check endpoint
 * @param metadata free-form instance metadata
 * @param lastHeartbeat instant of the last successful heartbeat
 */
public record InstanceResponse(
    String instanceId,
    String serviceId,
    String host,
    int port,
    String healthUrl,
    Map<String, String> metadata,
    Instant lastHeartbeat) {

  /** Maps a domain instance to its public representation. */
  public static InstanceResponse from(ServiceInstance instance) {
    return new InstanceResponse(
        instance.instanceId(),
        instance.serviceId(),
        instance.host(),
        instance.port(),
        instance.healthUrl(),
        instance.metadata(),
        instance.lastHeartbeat());
  }

  /** Maps a list of domain instances to public representations. */
  public static List<InstanceResponse> fromAll(List<ServiceInstance> instances) {
    return instances.stream().map(InstanceResponse::from).toList();
  }
}
