package com.integrity.interview.web.dto;

import com.integrity.interview.domain.InterviewMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/**
 * Request to create an interview.
 *
 * @param candidateId candidate identifier
 * @param candidateEmail candidate email address (for notification delivery only, not persisted)
 * @param candidateName candidate display name (for notification delivery only, not persisted)
 * @param recruiterId owning recruiter identifier
 * @param roundNumber interview round within the process
 * @param title interview title
 * @param mode delivery mode
 * @param meetingUrl optional meeting link
 * @param startsAt planned start instant
 * @param endsAt planned end instant
 * @param timezone IANA timezone of the interview
 * @param metadata free form JSON metadata
 */
public record CreateInterviewRequest(
    @NotNull UUID candidateId,
    @NotBlank @Size(max = 320) String candidateEmail,
    @Size(max = 200) String candidateName,
    @NotNull UUID recruiterId,
    @Min(1) @Max(100) int roundNumber,
    @NotBlank @Size(max = 250) String title,
    @NotNull InterviewMode mode,
    @Size(max = 500) String meetingUrl,
    @NotNull Instant startsAt,
    @NotNull Instant endsAt,
    @Size(max = 60) String timezone,
    @Size(max = 16000) String metadata) {}
