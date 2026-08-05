package com.integrity.interview.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Request to create a calendar event mirror.
 *
 * @param provider external calendar provider
 * @param providerEventId event id at the provider
 * @param eventUrl optional event link
 * @param startsAt event start instant
 * @param endsAt event end instant
 */
public record CreateCalendarEventRequest(
    @NotBlank @Size(max = 60) String provider,
    @NotBlank @Size(max = 255) String providerEventId,
    @Size(max = 500) String eventUrl,
    @NotNull Instant startsAt,
    @NotNull Instant endsAt) {}
