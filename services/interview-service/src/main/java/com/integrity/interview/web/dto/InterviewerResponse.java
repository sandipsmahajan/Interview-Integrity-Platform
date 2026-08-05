package com.integrity.interview.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public profile of an interviewer.
 *
 * @param id interviewer identifier
 * @param organizationId owning tenant
 * @param userId linked platform user
 * @param fullName interviewer name
 * @param email interviewer contact email
 * @param createdAt instant the profile was created
 */
public record InterviewerResponse(
    UUID id, UUID organizationId, UUID userId, String fullName, String email, Instant createdAt) {}
