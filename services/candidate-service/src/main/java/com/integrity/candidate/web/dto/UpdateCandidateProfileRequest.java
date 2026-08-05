package com.integrity.candidate.web.dto;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Request to update the extended candidate profile.
 *
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
 */
public record UpdateCandidateProfileRequest(
    @Size(max = 200) String headline,
    @Size(max = 2000) String bio,
    @Size(max = 120) String location,
    @Size(max = 60) String timezone,
    @Size(max = 4000) String resumeSummary,
    @Size(max = 255) String linkedinUrl,
    @Size(max = 255) String githubUrl,
    @Size(max = 50) List<String> skills,
    BigDecimal experienceYears,
    @Size(max = 8000) String attributes) {}
