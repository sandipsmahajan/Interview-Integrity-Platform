package com.interviewintegrity.recruiter.web.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request to update the extended recruiter profile.
 *
 * @param bio short biography
 * @param specialties areas of recruiting focus
 * @param linkedinUrl optional public profile URL
 * @param availability availability metadata
 */
public record UpdateRecruiterProfileRequest(
    @Size(max = 2000) String bio,
    @Size(max = 20) List<String> specialties,
    @Size(max = 255) String linkedinUrl,
    @Size(max = 1000) String availability) {}
