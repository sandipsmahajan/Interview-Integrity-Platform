package com.interviewintegrity.interview.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Request to update a calendar event mirror.
 *
 * @param eventUrl optional event link
 * @param startsAt event start instant
 * @param endsAt event end instant
 * @param status event status at the provider
 */
public record UpdateCalendarEventRequest(
    @Size(max = 500) String eventUrl,
    @NotNull Instant startsAt,
    @NotNull Instant endsAt,
    @Size(max = 60) String status) {}
