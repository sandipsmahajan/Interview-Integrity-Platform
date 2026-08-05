package com.integrity.notification.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to record a delivery outcome.
 *
 * @param provider provider name
 * @param providerMessageId provider message identifier
 * @param errorMessage failure detail
 */
public record DeliveryOutcomeRequest(
    @NotBlank @Size(max = 120) String provider,
    @Size(max = 255) String providerMessageId,
    @Size(max = 2000) String errorMessage) {}
