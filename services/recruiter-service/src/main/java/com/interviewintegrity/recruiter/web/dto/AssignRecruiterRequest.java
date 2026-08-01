package com.interviewintegrity.recruiter.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request to assign a recruiter to a candidate.
 *
 * @param recruiterId recruiting user
 * @param role role within the assignment
 */
public record AssignRecruiterRequest(
    @NotNull UUID recruiterId, @NotBlank @Size(max = 80) String role) {}
