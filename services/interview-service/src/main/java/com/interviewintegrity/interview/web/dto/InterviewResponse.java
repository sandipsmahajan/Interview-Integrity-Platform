package com.interviewintegrity.interview.web.dto;

import com.interviewintegrity.interview.domain.InterviewMode;
import com.interviewintegrity.interview.domain.InterviewStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of an interview.
 *
 * @param id interview identifier
 * @param organizationId owning tenant
 * @param candidateId candidate identifier
 * @param recruiterId owning recruiter identifier
 * @param roundNumber interview round within the process
 * @param title interview title
 * @param status lifecycle state
 * @param mode delivery mode
 * @param meetingUrl optional meeting link
 * @param startsAt planned start instant
 * @param endsAt planned end instant
 * @param timezone IANA timezone of the interview
 * @param metadata free form JSON metadata
 * @param createdAt instant the interview was created
 * @param updatedAt instant the interview was last modified
 */
public record InterviewResponse(
    UUID id,
    UUID organizationId,
    UUID candidateId,
    UUID recruiterId,
    int roundNumber,
    String title,
    InterviewStatus status,
    InterviewMode mode,
    String meetingUrl,
    Instant startsAt,
    Instant endsAt,
    String timezone,
    String metadata,
    Instant createdAt,
    Instant updatedAt) {}
