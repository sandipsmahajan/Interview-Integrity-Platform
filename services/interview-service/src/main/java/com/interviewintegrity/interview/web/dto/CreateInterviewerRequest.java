package com.interviewintegrity.interview.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request to create an interviewer profile.
 *
 * @param userId linked platform user
 * @param fullName interviewer name
 * @param email interviewer contact email
 */
public record CreateInterviewerRequest(
    @NotNull UUID userId,
    @NotBlank @Size(max = 150) String fullName,
    @NotBlank @Email @Size(max = 255) String email) {}
