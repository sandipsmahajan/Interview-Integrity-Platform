package com.integrity.candidate.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to update a candidate.
 *
 * @param fullName candidate display name
 * @param phone candidate contact phone
 * @param source acquisition source of the candidate
 */
public record UpdateCandidateRequest(
    @NotBlank @Size(max = 150) String fullName,
    @Size(max = 50) String phone,
    @Size(max = 120) String source) {}
