package com.interviewintegrity.discovery.domain;

import java.time.Instant;
import java.util.Map;

/**
 * A registered service instance in the registry.
 *
 * @param instanceId unique instance identifier
 * @param serviceId logical service name
 * @param host host the instance runs on
 * @param port port the instance listens on
 * @param healthUrl optional health check endpoint
 * @param metadata free-form instance metadata
 * @param lastHeartbeat instant of the last successful heartbeat
 */
public record ServiceInstance(
    String instanceId,
    String serviceId,
    String host,
    int port,
    String healthUrl,
    Map<String, String> metadata,
    Instant lastHeartbeat) {

  /** Returns the base address of the instance. */
  public String address() {
    return "http://" + host + ":" + port;
  }
}
