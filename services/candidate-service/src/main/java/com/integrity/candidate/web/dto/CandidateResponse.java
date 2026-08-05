package com.integrity.candidate.web.dto;

import com.integrity.candidate.domain.CandidateStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a candidate.
 *
 * @param id candidate identifier
 * @param organizationId owning tenant
 * @param userId linked platform user, when any
 * @param email candidate contact email
 * @param fullName candidate display name
 * @param phone candidate contact phone
 * @param status lifecycle status
 * @param source acquisition source
 * @param createdAt instant the candidate was created
 * @param updatedAt instant the candidate was last modified
 */
public record CandidateResponse(
    UUID id,
    UUID organizationId,
    UUID userId,
    String email,
    String fullName,
    String phone,
    CandidateStatus status,
    String source,
    Instant createdAt,
    Instant updatedAt) {}
