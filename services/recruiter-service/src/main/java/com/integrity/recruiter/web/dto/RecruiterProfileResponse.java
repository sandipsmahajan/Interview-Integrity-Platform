package com.integrity.recruiter.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public extended profile of a recruiter.
 *
 * @param id profile identifier
 * @param recruiterId owning recruiter
 * @param bio short biography
 * @param specialties areas of recruiting focus
 * @param linkedinUrl optional public profile URL
 * @param availability availability metadata
 * @param createdAt instant the profile was created
 */
public record RecruiterProfileResponse(
    UUID id,
    UUID recruiterId,
    String bio,
    List<String> specialties,
    String linkedinUrl,
    String availability,
    Instant createdAt) {}
