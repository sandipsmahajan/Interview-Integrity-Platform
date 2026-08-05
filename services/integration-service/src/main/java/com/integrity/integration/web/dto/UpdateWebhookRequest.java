package com.integrity.integration.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request to update a webhook subscription.
 *
 * @param url delivery endpoint
 * @param events subscribed events
 */
public record UpdateWebhookRequest(
    @NotBlank @Pattern(regexp = "^https://.*") @Size(max = 2000) String url,
    List<@Size(max = 255) String> events) {}
