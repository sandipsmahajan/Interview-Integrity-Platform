package com.interviewintegrity.recruiter.web.dto;

import com.interviewintegrity.recruiter.domain.RecruiterStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Public profile of a recruiter.
 *
 * @param id recruiter identifier
 * @param organizationId owning tenant
 * @param userId linked platform user
 * @param fullName recruiter name
 * @param email recruiter contact email
 * @param title job title
 * @param status working status
 * @param createdAt instant the profile was created
 */
public record RecruiterResponse(
    UUID id,
    UUID organizationId,
    UUID userId,
    String fullName,
    String email,
    String title,
    RecruiterStatus status,
    Instant createdAt) {}
