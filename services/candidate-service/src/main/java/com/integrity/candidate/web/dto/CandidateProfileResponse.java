package com.integrity.candidate.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public extended profile of a candidate.
 *
 * @param id profile identifier
 * @param candidateId owning candidate
 * @param headline professional headline
 * @param bio short biography
 * @param location candidate location
 * @param timezone candidate timezone
 * @param resumeSummary extracted resume summary
 * @param linkedinUrl optional public LinkedIn profile URL
 * @param githubUrl optional public GitHub profile URL
 * @param skills declared skill tags
 * @param experienceYears years of professional experience
 * @param attributes JSON encoded attribute bag
 * @param createdAt instant the profile was created
 * @param updatedAt instant the profile was last modified
 */
public record CandidateProfileResponse(
    UUID id,
    UUID candidateId,
    String headline,
    String bio,
    String location,
    String timezone,
    String resumeSummary,
    String linkedinUrl,
    String githubUrl,
    List<String> skills,
    BigDecimal experienceYears,
    String attributes,
    Instant createdAt,
    Instant updatedAt) {}
