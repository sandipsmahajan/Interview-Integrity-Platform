package com.integrity.candidate.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to update a candidate note.
 *
 * @param body note text
 */
public record UpdateCandidateNoteRequest(@NotBlank @Size(max = 4000) String body) {}
