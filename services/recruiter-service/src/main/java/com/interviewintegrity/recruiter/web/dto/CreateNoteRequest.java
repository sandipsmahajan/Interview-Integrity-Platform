package com.interviewintegrity.recruiter.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to create a note for a candidate.
 *
 * @param body note text
 */
public record CreateNoteRequest(@NotBlank @Size(max = 4000) String body) {}
