package com.integrity.candidate.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request to create a candidate.
 *
 * @param userId linked platform user, when any
 * @param email candidate contact email
 * @param fullName candidate display name
 * @param phone candidate contact phone
 * @param source acquisition source of the candidate
 */
public record CreateCandidateRequest(
    UUID userId,
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(max = 150) String fullName,
    @Size(max = 50) String phone,
    @Size(max = 120) String source) {}
