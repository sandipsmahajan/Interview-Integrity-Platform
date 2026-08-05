package com.integrity.recruiter.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a recruiter assignment.
 *
 * @param id assignment identifier
 * @param recruiterId assigned recruiter
 * @param candidateId subject candidate
 * @param role role within the assignment
 * @param assignedAt instant the assignment started
 * @param endedAt instant the assignment ended, when any
 */
public record RecruiterAssignmentResponse(
    UUID id,
    UUID recruiterId,
    UUID candidateId,
    String role,
    Instant assignedAt,
    Instant endedAt) {}
