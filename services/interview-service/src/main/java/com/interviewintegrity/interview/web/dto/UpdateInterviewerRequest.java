package com.interviewintegrity.interview.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to update an interviewer profile.
 *
 * @param fullName interviewer name
 * @param email interviewer contact email
 */
public record UpdateInterviewerRequest(
    @NotBlank @Size(max = 150) String fullName, @NotBlank @Email @Size(max = 255) String email) {}
