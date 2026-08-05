package com.integrity.interview.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Request to re-schedule an interview.
 *
 * @param startsAt planned start instant
 * @param endsAt planned end instant
 * @param timezone IANA timezone of the interview
 * @param meetingUrl optional meeting link
 */
public record ScheduleInterviewRequest(
    @NotNull Instant startsAt,
    @NotNull Instant endsAt,
    @Size(max = 60) String timezone,
    @Size(max = 500) String meetingUrl) {}
