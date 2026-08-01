package com.interviewintegrity.integration.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Request to create a webhook subscription.
 *
 * @param integrationId parent integration
 * @param url delivery endpoint
 * @param secretHash HMAC signing secret hash
 * @param events subscribed events
 */
public record CreateWebhookRequest(
    @NotNull UUID integrationId,
    @NotBlank @Pattern(regexp = "^https://.*") @Size(max = 2000) String url,
    @NotBlank @Size(max = 255) String secretHash,
    List<@Size(max = 255) String> events) {}
