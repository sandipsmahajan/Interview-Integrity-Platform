package com.interviewintegrity.interview.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a calendar event mirror.
 *
 * @param id event identifier
 * @param organizationId owning tenant
 * @param interviewId interview identifier
 * @param provider external calendar provider
 * @param providerEventId event id at the provider
 * @param eventUrl optional event link
 * @param startsAt event start instant
 * @param endsAt event end instant
 * @param status event status at the provider
 * @param createdAt instant the event was created
 * @param updatedAt instant the event was last modified
 */
public record InterviewCalendarEventResponse(
    UUID id,
    UUID organizationId,
    UUID interviewId,
    String provider,
    String providerEventId,
    String eventUrl,
    Instant startsAt,
    Instant endsAt,
    String status,
    Instant createdAt,
    Instant updatedAt) {}
