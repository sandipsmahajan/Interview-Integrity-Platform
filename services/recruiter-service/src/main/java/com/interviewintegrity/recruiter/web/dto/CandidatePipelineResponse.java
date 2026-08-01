package com.interviewintegrity.recruiter.web.dto;

import com.interviewintegrity.recruiter.domain.PipelineStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Public pipeline position of a candidate.
 *
 * @param id entry identifier
 * @param candidateId tracked candidate
 * @param recruiterId owning recruiter
 * @param stageId current stage
 * @param position position within the stage
 * @param status movement state
 * @param enteredAt instant the candidate entered the stage
 * @param exitedAt instant the candidate left the stage, when any
 */
public record CandidatePipelineResponse(
    UUID id,
    UUID candidateId,
    UUID recruiterId,
    UUID stageId,
    int position,
    PipelineStatus status,
    Instant enteredAt,
    Instant exitedAt) {}
