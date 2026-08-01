package com.interviewintegrity.recruiter.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to update a recruiter profile.
 *
 * @param fullName recruiter name
 * @param email recruiter contact email
 * @param title job title
 */
public record UpdateRecruiterRequest(
    @NotBlank @Size(max = 150) String fullName,
    @NotBlank @Email @Size(max = 255) String email,
    @Size(max = 120) String title) {}
