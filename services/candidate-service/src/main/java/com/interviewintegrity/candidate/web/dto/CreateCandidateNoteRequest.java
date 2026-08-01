package com.interviewintegrity.candidate.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to create a note for a candidate.
 *
 * @param body note text
 */
public record CreateCandidateNoteRequest(@NotBlank @Size(max = 4000) String body) {}
