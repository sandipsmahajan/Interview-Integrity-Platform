package com.interviewintegrity.discovery.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * Request to register a service instance.
 *
 * @param instanceId unique instance identifier
 * @param serviceId logical service name
 * @param host host the instance runs on
 * @param port port the instance listens on
 * @param healthUrl optional health check endpoint
 * @param metadata free-form instance metadata
 */
public record RegisterRequest(
    @NotBlank @Size(max = 120) String instanceId,
    @NotBlank @Size(max = 120) String serviceId,
    @NotBlank @Size(max = 255) String host,
    @NotNull @Min(1) @Max(65535) Integer port,
    @Size(max = 500) String healthUrl,
    Map<String, String> metadata) {}
