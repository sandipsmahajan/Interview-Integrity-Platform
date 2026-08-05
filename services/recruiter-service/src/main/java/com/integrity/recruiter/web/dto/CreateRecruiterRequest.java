package com.integrity.recruiter.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request to create a recruiter profile.
 *
 * @param userId linked platform user
 * @param fullName recruiter name
 * @param email recruiter contact email
 * @param title job title
 */
public record CreateRecruiterRequest(
    @NotNull UUID userId,
    @NotBlank @Size(max = 150) String fullName,
    @NotBlank @Email @Size(max = 255) String email,
    @Size(max = 120) String title) {}
